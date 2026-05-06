"""
Entraîne le pipeline Random Forest (TF-IDF + TruncatedSVD + RF) du notebook,
sans la partie SVM. Exporte artifacts/pipeline.joblib

Usage:
  python train_export.py --csv "C:/path/to/lamis_en_labeled.csv"
"""
from __future__ import annotations

import argparse
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.decomposition import TruncatedSVD
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import accuracy_score, classification_report, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

from preprocess import preprocess_for_model


def load_frame(csv_path: Path) -> pd.DataFrame:
    df = pd.read_csv(csv_path, encoding="utf-8")
    df["_phrase_group"] = df["reclamation_text"].astype(str).str.strip()
    df["reclamation_text"] = df["reclamation_text"].fillna(df["reclamation_text"].mode()[0])
    df["target"] = df["label"].astype(str).str.strip()
    df = df[df["target"] != "abnormal"].reset_index(drop=True)
    return df


def add_training_noise(text: str, rng: np.random.RandomState) -> str:
    t = str(text)
    if rng.random() < 0.2:
        return t + " problem"
    return t


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--csv", type=Path, required=True, help="CSV avec colonnes reclamation_text, label")
    p.add_argument("--out", type=Path, default=Path("artifacts/pipeline.joblib"))
    args = p.parse_args()

    df = load_frame(args.csv)
    rng = np.random.RandomState(42)

    df["reclamation_text"] = df["reclamation_text"].astype(str).map(preprocess_for_model)
    df["reclamation_text"] = df["reclamation_text"].apply(lambda t: add_training_noise(t, rng))

    X = df["reclamation_text"]
    y = df["target"]
    ph = df.drop_duplicates(subset=["_phrase_group"])[["_phrase_group", "target"]]
    train_groups, test_groups = train_test_split(
        ph["_phrase_group"].values,
        stratify=ph["target"],
        test_size=0.25,
        random_state=42,
    )
    tr = df["_phrase_group"].isin(train_groups)
    te = df["_phrase_group"].isin(test_groups)
    X_train, X_test = X.loc[tr], X.loc[te]
    y_train, y_test = y.loc[tr], y.loc[te]

    pipeline = Pipeline(
        [
            (
                "tfidf",
                TfidfVectorizer(
                    ngram_range=(1, 3),
                    min_df=1,
                    max_df=0.95,
                ),
            ),
            ("svd", TruncatedSVD(n_components=100, random_state=42)),
            (
                "rf",
                RandomForestClassifier(
                    n_estimators=200,
                    random_state=42,
                    n_jobs=-1,
                    class_weight="balanced",
                ),
            ),
        ]
    )
    pipeline.fit(X_train, y_train)
    y_pred = pipeline.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred, average="macro")
    print("Accuracy:", round(acc, 4))
    print("F1 macro:", round(f1, 4))
    print(classification_report(y_test, y_pred, zero_division=0))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, args.out)
    print("Saved:", args.out.resolve())


if __name__ == "__main__":
    main()
