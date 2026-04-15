"""
Prétraitement aligné sur le notebook (segmentation EN + nettoyage).
La section SVM du notebook n'est pas utilisée ici.
"""
from __future__ import annotations

import re
from functools import lru_cache

from wordfreq import top_n_list

NON_WORD = re.compile(r"[^a-z\s]")

en_words = set(top_n_list("en", 350_000))
extra_words = {
    "couldn't",
    "doesn't",
    "don't",
    "isn't",
    "wasn't",
    "hasn't",
    "homework",
    "download",
    "platform",
    "debited",
    "charged",
    "revising",
    "grading",
    "incomplete",
}
en_words.update(extra_words)


def normalize_reclamation(s: str) -> str:
    return str(s).replace("\u2019", "'").strip().lower()


def preprocess_reclamation_glued(t: str) -> str:
    t = normalize_reclamation(t)
    return " ".join(t.split())


def segment_concatenated_text(text: str, word_set: set, max_len: int = 32) -> str:
    s = preprocess_reclamation_glued(text)
    i = 0
    parts: list[str] = []
    while i < len(s):
        if s[i] in " \t\n":
            i += 1
            continue
        matched = False
        upper = min(len(s), i + max_len)
        for L in range(upper - i, 0, -1):
            w = s[i : i + L]
            if w in word_set:
                parts.append(w)
                i += L
                matched = True
                break
        if not matched:
            parts.append(s[i])
            i += 1
    return " ".join(parts)


@lru_cache(maxsize=300_000)
def reclamation_segmentee(text: str) -> str:
    return segment_concatenated_text(text, en_words)


def clean_reclamation(text: str) -> str:
    text = str(text).lower()
    text = text.replace("\u2019", " ")
    text = text.replace("'", " ")
    text = NON_WORD.sub(" ", text)
    return " ".join(text.split())


def preprocess_for_model(raw: str) -> str:
    """Chaîne prête pour TF-IDF (même logique qu'en inférence)."""
    seg = reclamation_segmentee(raw)
    return clean_reclamation(seg)
