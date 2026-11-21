package com.focusguard.study.filter

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Focus Input Filter ported directly from Focus-Control (focus-reminder.py).
 * Blocks explicit adult text from ever being typed or sent in the study browser.
 * Normalization defeats leetspeak, spacing, and zero-width obfuscations.
 */
object FocusInputFilter {

    const val INPUT_FILTER_NAME = "Focus Input Filter"

    // Zero-width characters to strip
    private val ZERO_WIDTH_REGEX = Pattern.compile("[\u200b-\u200f\u202a-\u202e\u2060\ufeff]")

    // Letter matching regex (latin letters, digits, and Bengali script range \u0980-\u09FF)
    private val LETTER_REGEX = Pattern.compile("[a-z0-9\u0980-\u09FF]")

    // Tokens too risky for compact substring matching -> whole-word match only
    val BLOCK_WORD_ONLY: Set<String> = setOf(
        "sex", "anal", "cum", "dick", "cock", "nude", "xxx",
        "magi", "randi", "bhabi", "mms", "fuck", "cuda"
    )

    // Leetspeak transliteration mapping
    private val LEET_MAP: Map<Char, Char> = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
        '6' to 'g', '7' to 't', '8' to 'b', '9' to 'g', '@' to 'a',
        '$' to 's', '!' to 'i', '|' to 'i'
    )

    val BLOCKED_INPUT_WORDS: List<String> = listOf(
        // English / international & slang
        "porn", "porno", "pornography", "xxx", "xnxx", "xvideo", "xvideos", "xhamster", "pornhub",
        "sex", "sexy", "sexting", "sexual", "nude", "nudes", "nudity", "naked", "nsfw",
        "fuck", "fucking", "fucker", "fucked",
        "cuda", "cudi", "cudar", "cudan", "cudacudi",
        "chuda", "choda", "chudi", "chudir", "chudar", "chodar", "chodon",
        "chudacudi", "chudachudi", "chodachudi", "chudai",
        "horny", "aroused", "orgasm", "erotic", "erotica", "hentai", "milf", "incest",
        "boobs", "boob", "pussy", "dick", "cock", "penis", "cum", "semen",
        "blowjob", "handjob", "anal", "anus", "masturbate", "masturbation",
        "slut", "whore", "prostitute", "onlyfans", "camgirl", "rapist", "condom",
        "viagra", "viral video", "viral mms",
        "boudi sex", "bhabi sex",
        "choti", "panu", "jouno", "jounota", "sexstory", "sexstories", "sexvideo",
        "sexchat", "sex golpo", "choti golpo", "panu golpo", "jouno golpo",
        "chudacudir golpo", "chudir golpo", "chodar golpo", "choda chudi golpo",
        "bangla choti", "bangla panu", "desi sex", "desi porn", "desi choti",
        "kamasutra", "kamsutra", "smut", "tits", "titties", "cumshot",
        "gangbang", "orgy", "threesome", "dildo", "vibrator", "fetish", "bondage",
        "stripper", "stripclub", "brothel", "magi", "khanki", "randi", "mms",
        "mms video", "blue film", "adult video", "adult story", "sex story",

        // Bangla script transliterations & equivalents
        "পর্ন", "পর্ণ", "পর্নো", "পর্ণো", "পর্নোগ্রাফি", "পর্ণোগ্রাফি",
        "এক্সএক্সএক্স", "এক্সএনএক্সএক্স", "এক্সভিডিও", "এক্সহ্যামস্টার", "পর্নহাব", "পর্ণহাব",
        "সেক্স", "সেক্সি", "সেক্সটিং", "সেক্সুয়াল", "সেক্সস্টোরি", "সেক্সভিডিও", "সেক্সচ্যাট", "সেক্স গল্প",
        "ন্যুড", "ন্যূড", "ন্যুডস", "ন্যূডস", "নেকেড", "নগ্ন", "উলঙ্গ",
        "ফাক", "ফাকিং", "ফাকার",
        "কুদা", "কুদি", "কুদাচুদি",
        "চুদা", "চুদি", "চোদন", "চুদাচুদি", "চোদা", "চোদাচুদি", "চুদাই",
        "হর্নি", "কামুক", "উত্তেজিত",
        "অর্গাজম", "বীর্যপাত",
        "ইরোটিক", "ইরোটিকা",
        "হেনতাই", "মিল্ফ", "মিলফ", "ইনসেস্ট",
        "বুবস", "বুব", "স্তন",
        "পুসি", "যোনি", "গুদ", "ভোদা",
        "ডিক", "কক", "পেনিস", "লেড়া",
        "কাম", "সিমেন", "বীর্য",
        "ব্লোজব", "হ্যান্ডজব",
        "অ্যানাল", "এনাল", "মলদ্বার",
        "মাস্টারবেশন", "হস্তমৈথুন",
        "স্লাট", "বেশ্যা", "পতিতা", "মাগি", "খানকি", "রান্ডি",
        "অনলিফ্যানস", "অনলিফ্যান্স", "ক্যামগার্ল",
        "ডিলডো", "ভাইব্রেটর",
        "গ্যাংব্যাং", "অর্জি", "থ্রিসাম",
        "স্ট্রিপার", "স্ট্রিপক্লাব",
        "ব্লু ফিল্ম", "ব্লুফিল্ম",
        "এডাল্ট ভিডিও", "এডাল্ট স্টোরি",
        "বৌদি সেক্স", "ভাবি সেক্স", "ভাইরাল ভিডিও",
        "চটি", "পানু", "যৌন", "যৌনতা", "যৌনগল্প", "চটি গল্প", "পানু গল্প", "যৌন গল্প",
        "অশ্লীল", "কামসূত্র"
    )

    private data class BlockNorm(val rawWord: String, val normWord: String)
    private data class BlockCompact(val rawWord: String, val compactWord: String)

    private val customKeywords = mutableSetOf<String>()
    private var blockNormList: List<BlockNorm> = emptyList()
    private var blockCompactList: List<BlockCompact> = emptyList()

    init {
        rebuildLists()
    }

    @Synchronized
    internal fun setCustomKeywords(keywords: Set<String>) {
        customKeywords.clear()
        customKeywords.addAll(
            keywords.map { it.trim().lowercase() }
                .filter { it.length >= 3 && !it.all { ch -> ch == 'x' } }
        )
        rebuildLists()
    }

    @Synchronized
    internal fun addCustomKeyword(keyword: String) {
        val clean = keyword.trim().lowercase()
        if (clean.length >= 3 && !clean.all { ch -> ch == 'x' }) {
            customKeywords.add(clean)
            rebuildLists()
        }
    }

    @Synchronized
    internal fun clearCustomKeywords() {
        customKeywords.clear()
        rebuildLists()
    }

    @Synchronized
    private fun rebuildLists() {
        val normList = mutableListOf<BlockNorm>()
        val compactList = mutableListOf<BlockCompact>()
        val allWords = (BLOCKED_INPUT_WORDS + customKeywords).distinct()

        for (w in allWords) {
            val n = inputNorm(w)
            val cleaned = n.replace(Regex("[^a-z0-9\u0980-\u09FF]+"), " ").trim()
            normList.add(BlockNorm(w, cleaned))

            val (c1, _) = compactMap(w, collapse = false)
            val (c2, _) = compactMap(w, collapse = true)
            val forms = setOf(c1, c2)
            for (form in forms) {
                if (form.isNotEmpty()) {
                    compactList.add(BlockCompact(w, form))
                }
            }
        }
        blockNormList = normList
        blockCompactList = compactList
    }

    fun inputNorm(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        var s = text.lowercase()
        s = ZERO_WIDTH_REGEX.matcher(s).replaceAll("")
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
        val sb = StringBuilder()
        for (i in 0 until s.length) {
            val ch = s[i]
            val type = Character.getType(ch)
            // Strip Latin combining diacritics but preserve Bengali vowel marks (\u0980-\u09FF)
            if (type == Character.NON_SPACING_MARK.toInt() && ch.code < 0x0900) {
                continue
            }
            sb.append(LEET_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun compactMap(text: String, collapse: Boolean): Pair<String, BooleanArray> {
        val n = inputNorm(text)
        val chars = ArrayList<Char>(n.length)
        val gaps = ArrayList<Boolean>(n.length)
        var pendingGap = true

        for (i in 0 until n.length) {
            val ch = n[i]
            if (LETTER_REGEX.matcher(ch.toString()).matches()) {
                chars.add(ch)
                gaps.add(pendingGap)
                pendingGap = false
            } else {
                pendingGap = true
            }
        }

        if (collapse) {
            val cc = ArrayList<Char>(chars.size)
            val cg = ArrayList<Boolean>(gaps.size)
            for (i in 0 until chars.size) {
                if (cc.isNotEmpty() && chars[i] == cc[cc.size - 1] && !gaps[i]) {
                    continue
                }
                cc.add(chars[i])
                cg.add(gaps[i])
            }
            return Pair(cc.joinToString(""), cg.toBooleanArray())
        }

        return Pair(chars.joinToString(""), gaps.toBooleanArray())
    }

    private fun compactVariants(text: String): List<Pair<String, BooleanArray>> {
        val raw = compactMap(text, collapse = false)
        val col = compactMap(text, collapse = true)
        return if (raw.first == col.first) listOf(raw) else listOf(raw, col)
    }

    private fun isBangla(word: String): Boolean {
        for (i in 0 until word.length) {
            val ch = word[i]
            if (ch in '\u0980'..'\u09FF') return true
        }
        return false
    }

    /**
     * Checks if the text contains any explicit or blocked input keyword.
     * Returns the matched word, or null if clean.
     */
    fun explicitHit(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        // Safety guard: 1-character and 2-character Latin inputs (e.g. 'x', 'xx', 'a') can NEVER be explicit hits
        if (trimmed.length < 3 && !isBangla(trimmed)) {
            return null
        }

        val spaced = " " + inputNorm(text).replace(Regex("[^a-z0-9\u0980-\u09FF]+"), " ").trim() + " "
        for (item in blockNormList) {
            if (item.normWord.isNotEmpty() && spaced.contains(" " + item.normWord + " ")) {
                return item.rawWord
            }
        }

        val variants = compactVariants(text)
        for (item in blockCompactList) {
            val word = item.rawWord
            val compWord = item.compactWord
            if (compWord.isEmpty()) continue

            val minLen = when {
                isBangla(compWord) -> 2
                BLOCK_WORD_ONLY.contains(word) -> 3
                else -> 4
            }
            if (compWord.length < minLen) continue

            for (pair in variants) {
                val comp = pair.first
                val gaps = pair.second
                var start = 0
                while (true) {
                    val at = comp.indexOf(compWord, start)
                    if (at < 0) break
                    val end = at + compWord.length

                    // Strict word boundary checks to eliminate in-word blocking:
                    // Left boundary: starts at index 0 or was preceded by a non-letter/whitespace gap
                    val isLeftBoundary = (at == 0) || (at < gaps.size && gaps[at])
                    // Right boundary: ends at text end or was followed by a non-letter/whitespace gap
                    val isRightBoundary = (end >= comp.length) || (end < gaps.size && gaps[end])

                    // Match ONLY if bounded on both sides (never block internal substrings of legitimate words)
                    if (isLeftBoundary && isRightBoundary) {
                        return word
                    }

                    start = at + 1
                }
            }
        }
        return null
    }

    fun isBlocked(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val rawLower = raw.trim().lowercase()
        for (w in BLOCKED_INPUT_WORDS) {
            if (rawLower == w.lowercase()) return true
        }
        synchronized(this) {
            for (w in customKeywords) {
                if (rawLower == w) return true
            }
        }
        return explicitHit(rawLower) != null
    }

    /**
     * Builds the client-side JavaScript input filter to inject into the WebView.
     * This faithfully implements Focus-Control's `_build_input_filter_js()`.
     */
    fun buildInputFilterJs(): String {
        val allWords = synchronized(this) {
            (BLOCKED_INPUT_WORDS + customKeywords).distinct()
        }
        val jsonWords = org.json.JSONArray(allWords).toString()
        return """
(function () {
  if (window.__focusInputFilter) { return; }
  window.__focusInputFilter = true;
  var RAW = $jsonWords;
  var ZERO = /[\u200b-\u200f\u202a-\u202e\u2060\ufeff]/g;
  var LEET = {'0':'o','1':'i','3':'e','4':'a','5':'s','6':'g','7':'t','8':'b','9':'g',
              '@':'a','$':'s','!':'i','|':'i'};
  function norm(s) {
    s = String(s || '').toLowerCase().replace(ZERO, '');
    try { s = s.normalize('NFKD').replace(/[\u0300-\u036f]/g, ''); } catch (e) {}
    return s.replace(/[013456789@$!|]/g, function (c) { return LEET[c] || c; });
  }
  function spaced(s) {
    return ' ' + norm(s).replace(/[^a-z0-9\u0980-\u09FF]+/g, ' ').trim() + ' ';
  }
  function sqMap(s, collapse) {
    var n = norm(s), chars = [], gaps = [], pending = true, i;
    for (i = 0; i < n.length; i++) {
      var ch = n[i];
      if (/[a-z0-9\u0980-\u09FF]/.test(ch)) { chars.push(ch); gaps.push(pending); pending = false; }
      else { pending = true; }
    }
    if (collapse) {
      var cc = [], cg = [];
      for (i = 0; i < chars.length; i++) {
        if (cc.length && chars[i] === cc[cc.length - 1] && !gaps[i]) { continue; }
        cc.push(chars[i]); cg.push(gaps[i]);
      }
      chars = cc; gaps = cg;
    }
    return { c: chars.join(''), g: gaps };
  }
  function sqVariants(s) {
    var raw = sqMap(s, false), col = sqMap(s, true);
    return raw.c === col.c ? [raw] : [raw, col];
  }
  var BAN = RAW.map(norm);
  var BANC = [];
  RAW.forEach(function (w) {
    var seen = {};
    [sqMap(w, false).c, sqMap(w, true).c].forEach(function (c) {
      if (c && !seen[c]) { seen[c] = 1; BANC.push({ w: w, c: c }); }
    });
  });
  window.__focusAddWord = function (w) {
    if (!w) { return; }
    w = String(w).toLowerCase();
    if (RAW.indexOf(w) === -1) {
      RAW.push(w);
      BAN.push(norm(w));
      var seen = {};
      [sqMap(w, false).c, sqMap(w, true).c].forEach(function (c) {
        if (c && !seen[c]) { seen[c] = 1; BANC.push({ w: w, c: c }); }
      });
    }
  };
  var WORD_ONLY = {'sex':1,'anal':1,'cum':1,'dick':1,'cock':1,'nude':1,'xxx':1,
                   'magi':1,'randi':1,'bhabi':1,'mms':1,'fuck':1,'cuda':1};
  function hit(text) {
    if (!text) { return null; }
    var t = spaced(text);
    var i;
    for (i = 0; i < BAN.length; i++) {
      var w = BAN[i].replace(/[^a-z0-9\u0980-\u09FF]+/g, ' ').trim();
      if (w && t.indexOf(' ' + w + ' ') !== -1) { return RAW[i]; }
    }
    var variants = sqVariants(text);
    for (i = 0; i < BANC.length; i++) {
      var word = BANC[i].w, cw = BANC[i].c;
      var minL = /[\u0980-\u09FF]/.test(cw) ? 2 : (WORD_ONLY[word] ? 3 : 4);
      if (cw.length < minL) { continue; }
      for (var v = 0; v < variants.length; v++) {
        var cm = variants[v], pos = 0;
        while (true) {
          var at = cm.c.indexOf(cw, pos);
          if (at < 0) { break; }
          var end = at + cw.length;

          // Strict boundary check to eliminate in-word blocking
          var isLeftBoundary = (at === 0) || (at < cm.g.length && cm.g[at]);
          var isRightBoundary = (end >= cm.c.length) || (end < cm.g.length && cm.g[end]);

          if (isLeftBoundary && isRightBoundary) {
            return word;
          }
          pos = at + 1;
        }
      }
    }
    return null;
  }
  window.__focusFilterHit = hit;

  var warned = 0;
  function toast(msg) {
    var now = Date.now();
    if (now - warned < 1000) { return; }
    warned = now;
    var d = document.getElementById('__focus_filter_toast');
    if (!d) {
      d = document.createElement('div');
      d.id = '__focus_filter_toast';
      d.style.cssText = 'position:fixed;top:14px;left:50%;transform:translateX(-50%);' +
        'z-index:2147483647;background:#7f1d1d;color:#fff;font:bold 14px sans-serif;' +
        'padding:10px 18px;border-radius:10px;box-shadow:0 6px 20px rgba(0,0,0,.55);' +
        'pointer-events:none;max-width:80vw;';
      (document.body || document.documentElement).appendChild(d);
    }
    d.textContent = '⛔ Focus Input Filter: ' + msg;
    d.style.display = 'block';
    clearTimeout(d.__t);
    d.__t = setTimeout(function () { d.style.display = 'none'; }, 2600);
  }

  function editable(el) {
    return !!el && (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT' || el.isContentEditable);
  }
  function val(el) { return el.isContentEditable ? el.innerText : el.value; }
  var lastGood = new WeakMap();

  document.addEventListener('focusin', function (e) {
    var el = e.target;
    if (editable(el)) { lastGood.set(el, el.isContentEditable ? el.innerHTML : el.value); }
  }, true);

  document.addEventListener('input', function (e) {
    var el = e.target;
    if (!editable(el)) { return; }
    var bad = hit(val(el));
    if (bad) {
      var good = lastGood.get(el);
      if (good !== undefined) {
        if (el.isContentEditable) { el.innerHTML = good; } else { el.value = good; }
      }
      toast('explicit content blocked (' + bad + ')');
      if (window.FocusInputBridge && window.FocusInputBridge.onExplicitAttempt) {
        window.FocusInputBridge.onExplicitAttempt(bad);
      }
      return;
    }
    lastGood.set(el, el.isContentEditable ? el.innerHTML : el.value);
  }, true);

  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter' || !editable(e.target)) { return; }
    var badEnter = hit(val(e.target));
    if (badEnter) {
      e.preventDefault();
      e.stopImmediatePropagation();
      toast('message not allowed');
      if (window.FocusInputBridge && window.FocusInputBridge.onExplicitAttempt) {
        window.FocusInputBridge.onExplicitAttempt(badEnter);
      }
    }
  }, true);
})();
        """.trimIndent()
    }
}
