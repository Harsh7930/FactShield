#!/usr/bin/env python3
"""
FactShield — classify text with openai-community/roberta-base-openai-detector.

Reads the news string from argv[1] (ProcessBuilder passes it as a single argument).
Prints exactly one line to stdout: ``Real`` or ``Fake``, matching the model's
``id2label`` mapping (0 -> Fake, 1 -> Real). The final line includes the
confidence score from the pipeline, e.g. ``Fake 0.9873``.

Setup::
    python3 -m pip install transformers torch
"""

from __future__ import annotations

import sys

sys.path.insert(0, '/Users/sakshamkatiyar/Library/Python/3.9/lib/python/site-packages')

MODEL_ID = "openai-community/roberta-base-openai-detector"


def main() -> None:
    text = sys.argv[1] if len(sys.argv) > 1 else ""
    text = text.strip()
    if not text:
        # No model run — report full confidence for trivial empty case
        print("Real 1.0", flush=True)
        return

    from transformers import pipeline

    clf = pipeline(
        "text-classification",
        model=MODEL_ID,
        truncation=True,
        max_length=512,
    )
    result = clf(text[:50_000])[0]
    label = str(result.get("label", "")).strip().lower()
    score = float(result.get("score", 0.0))

    if label == "real":
        print(f"Real {score}", flush=True)
    elif label == "fake":
        print(f"Fake {score}", flush=True)
    else:
        raise ValueError(f"Unexpected model label: {result!r}")


if __name__ == "__main__":
    main()
