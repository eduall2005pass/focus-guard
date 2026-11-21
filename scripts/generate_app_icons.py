#!/usr/bin/env python3
import os
import math
from PIL import Image, ImageDraw, ImageFilter

def create_icon_master(size=1024, is_round=False):
    # Create master image
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # 1. Base Gradient Canvas
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg)
    
    cx, cy = size / 2, size / 2
    max_radius = size * 0.7
    
    # Draw radial gradient
    # Top/Center: Deep vibrant emerald (#0C4A34), Edges: Deep obsidian emerald (#02150E)
    for r_step in range(int(max_radius), 0, -4):
        factor = r_step / max_radius
        r = int(12 * (1 - factor) + 2 * factor)
        g = int(74 * (1 - factor) + 21 * factor)
        b = int(52 * (1 - factor) + 14 * factor)
        bbox = [cx - r_step, cy - r_step * 0.9, cx + r_step, cy + r_step * 1.1]
        bg_draw.ellipse(bbox, fill=(r, g, b, 255))
        
    # Subtle geometric tech rings for modern cyber-guardian feel
    for ring_r in [size * 0.44, size * 0.38, size * 0.30]:
        bg_draw.ellipse(
            [cx - ring_r, cy - ring_r, cx + ring_r, cy + ring_r],
            outline=(16, 185, 129, 25),
            width=2
        )
        
    # 2. Draw Shield Layer
    shield_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shield_draw = ImageDraw.Draw(shield_layer)
    
    # Shield coordinates (relative to 1024)
    # Top arc, high shoulders, sleek tapered base
    top_y = size * 0.22
    shoulder_y = size * 0.30
    waist_y = size * 0.52
    bottom_y = size * 0.82
    left_x = size * 0.22
    right_x = size * 0.78
    
    shield_points = []
    # Top edge curve (from left shoulder to top center to right shoulder)
    steps = 20
    for i in range(steps + 1):
        t = i / steps
        x = left_x + t * (right_x - left_x)
        # Gentle curve dipping towards top_y in center
        curve_offset = math.sin(t * math.pi) * (size * 0.04)
        y = shoulder_y - curve_offset
        shield_points.append((x, y))
        
    # Right side down to bottom point
    for i in range(steps + 1):
        t = i / steps
        # Bezier-like curve
        x = (1 - t)**2 * right_x + 2 * (1 - t) * t * (right_x - size * 0.04) + t**2 * cx
        y = (1 - t)**2 * shoulder_y + 2 * (1 - t) * t * waist_y + t**2 * bottom_y
        shield_points.append((x, y))
        
    # Bottom point to left shoulder
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t)**2 * cx + 2 * (1 - t) * t * (left_x + size * 0.04) + t**2 * left_x
        y = (1 - t)**2 * bottom_y + 2 * (1 - t) * t * waist_y + t**2 * shoulder_y
        shield_points.append((x, y))

    # Outer Shield Glow
    glow_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_draw.polygon(shield_points, fill=(16, 185, 129, 120))
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(radius=18))
    
    # Shield Body Fill (Dark Forest Emerald with subtle bevel)
    shield_draw.polygon(shield_points, fill=(6, 38, 27, 245))
    
    # Right facet / ambient lighting on shield
    right_facet = [(cx, top_y + size * 0.02)]
    for pt in shield_points:
        if pt[0] >= cx:
            right_facet.append(pt)
    right_facet.append((cx, bottom_y))
    shield_draw.polygon(right_facet, fill=(4, 28, 20, 100))
    
    # Left facet highlight
    left_facet = [(cx, top_y + size * 0.02)]
    for pt in shield_points:
        if pt[0] <= cx:
            left_facet.append(pt)
    left_facet.append((cx, bottom_y))
    shield_draw.polygon(left_facet, fill=(16, 185, 129, 25))

    # Shield Outer Glowing Border
    shield_draw.line(shield_points + [shield_points[0]], fill=(52, 211, 153, 255), width=int(size * 0.014))
    
    # Shield Inner Accent Border
    inner_scale = 0.88
    inner_points = []
    for px, py in shield_points:
        ix = cx + (px - cx) * inner_scale
        iy = (cy + size * 0.01) + (py - (cy + size * 0.01)) * inner_scale
        inner_points.append((ix, iy))
    shield_draw.line(inner_points + [inner_points[0]], fill=(16, 185, 129, 90), width=int(size * 0.005))

    # 3. Draw Radiant Emerald Sprout / Leaf (Focus & Wellness Emblem)
    leaf_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    leaf_draw = ImageDraw.Draw(leaf_layer)
    
    # Primary Leaf (curves gracefully up and right)
    primary_leaf_pts = []
    base_x, base_y = cx, size * 0.68
    tip_x, tip_y = cx + size * 0.14, size * 0.38
    
    # Right edge of leaf
    steps = 25
    for i in range(steps + 1):
        t = i / steps
        # Quadratic curve out to right
        ctrl_x, ctrl_y = cx + size * 0.22, size * 0.58
        x = (1 - t)**2 * base_x + 2 * (1 - t) * t * ctrl_x + t**2 * tip_x
        y = (1 - t)**2 * base_y + 2 * (1 - t) * t * ctrl_y + t**2 * tip_y
        primary_leaf_pts.append((x, y))
        
    # Left edge of leaf curving back
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx + size * 0.01, size * 0.46
        x = (1 - t)**2 * tip_x + 2 * (1 - t) * t * ctrl_x + t**2 * base_x
        y = (1 - t)**2 * tip_y + 2 * (1 - t) * t * ctrl_y + t**2 * base_y
        primary_leaf_pts.append((x, y))
        
    leaf_draw.polygon(primary_leaf_pts, fill=(16, 185, 129, 255))
    
    # Inner gradient / highlight on primary leaf
    leaf_highlight_pts = []
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx + size * 0.16, size * 0.54
        x = (1 - t)**2 * base_x + 2 * (1 - t) * t * ctrl_x + t**2 * tip_x
        y = (1 - t)**2 * base_y + 2 * (1 - t) * t * ctrl_y + t**2 * tip_y
        leaf_highlight_pts.append((x, y))
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx + size * 0.05, size * 0.48
        x = (1 - t)**2 * tip_x + 2 * (1 - t) * t * ctrl_x + t**2 * base_x
        y = (1 - t)**2 * tip_y + 2 * (1 - t) * t * ctrl_y + t**2 * base_y
        leaf_highlight_pts.append((x, y))
    leaf_draw.polygon(leaf_highlight_pts, fill=(52, 211, 153, 240))
    
    # Crisp Leaf Central Vein (Pure White #FFFFFF)
    vein_pts = []
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx + size * 0.10, size * 0.52
        x = (1 - t)**2 * (base_x - size * 0.005) + 2 * (1 - t) * t * ctrl_x + t**2 * tip_x
        y = (1 - t)**2 * base_y + 2 * (1 - t) * t * ctrl_y + t**2 * tip_y
        vein_pts.append((x, y))
    leaf_draw.line(vein_pts, fill=(255, 255, 255, 240), width=int(size * 0.009))

    # Secondary Companion Leaf (curves gently to the left)
    sec_leaf_pts = []
    sec_tip_x, sec_tip_y = cx - size * 0.12, size * 0.48
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx - size * 0.14, size * 0.62
        x = (1 - t)**2 * base_x + 2 * (1 - t) * t * ctrl_x + t**2 * sec_tip_x
        y = (1 - t)**2 * (base_y - size * 0.03) + 2 * (1 - t) * t * ctrl_y + t**2 * sec_tip_y
        sec_leaf_pts.append((x, y))
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx - size * 0.02, size * 0.55
        x = (1 - t)**2 * sec_tip_x + 2 * (1 - t) * t * ctrl_x + t**2 * base_x
        y = (1 - t)**2 * sec_tip_y + 2 * (1 - t) * t * ctrl_y + t**2 * (base_y - size * 0.03)
        sec_leaf_pts.append((x, y))
    leaf_draw.polygon(sec_leaf_pts, fill=(5, 150, 105, 245))
    
    sec_vein_pts = []
    for i in range(steps + 1):
        t = i / steps
        ctrl_x, ctrl_y = cx - size * 0.06, size * 0.58
        x = (1 - t)**2 * base_x + 2 * (1 - t) * t * ctrl_x + t**2 * sec_tip_x
        y = (1 - t)**2 * (base_y - size * 0.03) + 2 * (1 - t) * t * ctrl_y + t**2 * sec_tip_y
        sec_vein_pts.append((x, y))
    leaf_draw.line(sec_vein_pts, fill=(255, 255, 255, 180), width=int(size * 0.006))

    # 4. Brilliant Guardian Star / Diamond at the top apex of the Shield
    star_cy = size * 0.33
    star_r_long = size * 0.042
    star_r_short = size * 0.016
    star_points = [
        (cx, star_cy - star_r_long),
        (cx + star_r_short, star_cy - star_r_short),
        (cx + star_r_long, star_cy),
        (cx + star_r_short, star_cy + star_r_short),
        (cx, star_cy + star_r_long),
        (cx - star_r_short, star_cy + star_r_short),
        (cx - star_r_long, star_cy),
        (cx - star_r_short, star_cy - star_r_short),
    ]
    
    # Star glow
    star_glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    star_glow_draw = ImageDraw.Draw(star_glow)
    star_glow_draw.polygon(star_points, fill=(110, 231, 183, 200))
    star_glow = star_glow.filter(ImageFilter.GaussianBlur(radius=8))
    
    leaf_draw.polygon(star_points, fill=(255, 255, 255, 255))
    
    # 5. Composite all layers
    img = Image.alpha_composite(img, bg)
    img = Image.alpha_composite(img, glow_layer)
    img = Image.alpha_composite(img, shield_layer)
    img = Image.alpha_composite(img, star_glow)
    img = Image.alpha_composite(img, leaf_layer)
    
    # 6. Apply round mask if requested (for ic_launcher_round)
    if is_round:
        mask = Image.new("L", (size, size), 0)
        mask_draw = ImageDraw.Draw(mask)
        mask_draw.ellipse([size * 0.02, size * 0.02, size * 0.98, size * 0.98], fill=255)
        round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_img.paste(img, (0, 0), mask)
        img = round_img
    else:
        # Subtle squircle rounded corners for standard non-adaptive launcher fallback
        mask = Image.new("L", (size, size), 0)
        mask_draw = ImageDraw.Draw(mask)
        corner_r = int(size * 0.22)
        mask_draw.rounded_rectangle([size * 0.02, size * 0.02, size * 0.98, size * 0.98], radius=corner_r, fill=255)
        squircle_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        squircle_img.paste(img, (0, 0), mask)
        img = squircle_img
        
    return img

def export_icons():
    base_res = "app/src/main/res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    
    print("[*] Generating master 1024x1024 Focus Guard emblem...")
    master_square = create_icon_master(size=1024, is_round=False)
    master_round = create_icon_master(size=1024, is_round=True)
    
    # Save 512x512 master store icon
    store_dir = "app/src/main"
    master_square.resize((512, 512), Image.Resampling.LANCZOS).save(f"{store_dir}/ic_launcher-web.png")
    print(f"[+] Saved {store_dir}/ic_launcher-web.png (512x512)")
    
    for folder, dim in densities.items():
        dir_path = os.path.join(base_res, folder)
        os.makedirs(dir_path, exist_ok=True)
        
        # Save standard icon
        sq_img = master_square.resize((dim, dim), Image.Resampling.LANCZOS)
        sq_path = os.path.join(dir_path, "ic_launcher.png")
        sq_img.save(sq_path, "PNG")
        
        # Save round icon
        rd_img = master_round.resize((dim, dim), Image.Resampling.LANCZOS)
        rd_path = os.path.join(dir_path, "ic_launcher_round.png")
        rd_img.save(rd_path, "PNG")
        
        print(f"[+] Generated {folder}: {dim}x{dim}")

if __name__ == "__main__":
    export_icons()
