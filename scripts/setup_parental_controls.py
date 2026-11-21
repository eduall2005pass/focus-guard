#!/usr/bin/env python3
import time
import subprocess
import re
import xml.etree.ElementTree as ET

def run(cmd):
    p = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return p.stdout.strip(), p.stderr.strip(), p.returncode

def dump_ui():
    run("adb shell uiautomator dump /data/local/tmp/dump.xml")
    out, _, code = run("adb shell cat /data/local/tmp/dump.xml")
    if code != 0 or not out:
        return []
    try:
        root = ET.fromstring(out)
    except Exception:
        return []

    nodes = []
    for n in root.iter("node"):
        bounds = n.attrib.get("bounds", "")
        center = None
        if bounds:
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                center = ((x1 + x2) // 2, (y1 + y2) // 2)
        nodes.append({
            "text": n.attrib.get("text", ""),
            "desc": n.attrib.get("content-desc", ""),
            "res": n.attrib.get("resource-id", ""),
            "cls": n.attrib.get("class", ""),
            "bounds": bounds,
            "center": center,
            "clickable": n.attrib.get("clickable", "") == "true",
            "checked": n.attrib.get("checked", "") == "true"
        })
    return nodes

def find_node(nodes, text=None, text_contains=None, desc_contains=None, res_contains=None, cls=None):
    for n in nodes:
        if text and n["text"].strip().lower() == text.strip().lower():
            return n
        if text_contains and text_contains.lower() in n["text"].lower():
            return n
        if desc_contains and desc_contains.lower() in n["desc"].lower():
            return n
        if res_contains and res_contains.lower() in n["res"].lower():
            return n
        if cls and n["cls"] == cls:
            return n
    return None

def tap_node(node):
    if node and node["center"]:
        x, y = node["center"]
        print(f"[*] Tapping ({x}, {y}) - text: {node['text']!r}, res: {node['res']!r}")
        run(f"adb shell input tap {x} {y}")
        time.sleep(1)
        return True
    return False

def setup():
    print("[*] Waiting for ADB device...")
    run("adb wait-for-device")
    print("[+] Device connected!")
    
    # Wake up device
    run("adb shell input keyevent KEYCODE_WAKEUP")
    run("adb shell wm dismiss-keyguard")
    time.sleep(1)

    PIN = "159333"

    for step in range(15):
        nodes = dump_ui()
        if not nodes:
            print(f"[!] Step {step}: Could not dump UI, waiting...")
            time.sleep(1)
            continue

        texts = [n["text"] for n in nodes if n["text"]]
        print(f"[*] Step {step}: Screen texts: {texts[:8]}")

        # Case 1: On PIN setup screen (Set a PIN / Re-enter PIN / Confirm PIN)
        pin_field = find_node(nodes, res_contains="password_entry") or find_node(nodes, cls="android.widget.EditText")
        btn_continue = find_node(nodes, text="Continue") or find_node(nodes, text="Next") or find_node(nodes, text="চালিয়ে যান")
        btn_ok = find_node(nodes, text="OK") or find_node(nodes, text="Done") or find_node(nodes, text="Confirm") or find_node(nodes, text="ঠিক আছে")

        if any("re-enter" in t.lower() or "confirm" in t.lower() or "পুনরায়" in t.lower() for t in texts):
            print("[*] Detected PIN Confirmation Screen. Typing PIN again...")
            if pin_field:
                tap_node(pin_field)
            run(f"adb shell input text {PIN}")
            time.sleep(0.5)
            if btn_ok:
                tap_node(btn_ok)
            elif btn_continue:
                tap_node(btn_continue)
            else:
                run("adb shell input keyevent 66") # Enter
            time.sleep(1.5)
            continue

        if any("set a pin" in t.lower() or "enter a pin" in t.lower() or "পিন" in t.lower() for t in texts) and (btn_continue or pin_field):
            print(f"[*] Setting Parental Controls PIN to {PIN}...")
            if pin_field:
                tap_node(pin_field)
            run(f"adb shell input text {PIN}")
            time.sleep(0.5)
            if btn_continue:
                tap_node(btn_continue)
            elif btn_ok:
                tap_node(btn_ok)
            else:
                run("adb shell input keyevent 66") # Enter
            time.sleep(1.5)
            continue

        # Case 2: In Web Content Filters screen
        node_block_explicit = (
            find_node(nodes, text_contains="block explicit") or
            find_node(nodes, text_contains="Try to block explicit") or
            find_node(nodes, text_contains="অশ্লীল")
        )
        if node_block_explicit:
            print("[+] Found Web Content Filter: Block Explicit Sites!")
            tap_node(node_block_explicit)
            time.sleep(1)
            # Check if there is a save/done button
            save_btn = find_node(nodes, text="Save") or find_node(nodes, text="Done")
            if save_btn:
                tap_node(save_btn)
            print("[SUCCESS] Web content filtering set to Block Explicit Sites!")
            return True

        # Case 3: In Parental Controls Dashboard
        node_web_filters = find_node(nodes, text_contains="Web content filter")
        node_setup_pin = find_node(nodes, text_contains="Set up a parental controls PIN")

        if node_setup_pin:
            print("[*] Clicking 'Set up a parental controls PIN'...")
            tap_node(node_setup_pin)
            time.sleep(1)
            continue

        if node_web_filters:
            print("[*] Clicking 'Web content filters'...")
            tap_node(node_web_filters)
            time.sleep(1)
            # If prompted for PIN to open Web content filters
            time.sleep(0.5)
            pin_check = dump_ui()
            p_field = find_node(pin_check, res_contains="password_entry") or find_node(pin_check, cls="android.widget.EditText")
            if p_field or any("pin" in n["text"].lower() for n in pin_check):
                print(f"[*] Prompted for PIN to access Web filters. Entering {PIN}...")
                if p_field:
                    tap_node(p_field)
                run(f"adb shell input text {PIN}")
                time.sleep(0.5)
                ok_btn = find_node(pin_check, text="OK") or find_node(pin_check, text="Continue")
                if ok_btn:
                    tap_node(ok_btn)
                else:
                    run("adb shell input keyevent 66")
                time.sleep(1)
            continue

        # If on Settings main page or somewhere else, launch Parental controls
        print("[*] Navigating to Parental controls...")
        run("adb shell am start -n com.android.settings/.supervision.SupervisionDashboardActivity")
        time.sleep(1.5)

    return False

if __name__ == "__main__":
    setup()
