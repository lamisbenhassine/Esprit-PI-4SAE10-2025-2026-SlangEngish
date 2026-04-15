"""
Service HTTP d'inférence pour le microservice Java (Random Forest uniquement).
Définir RECLAMATION_ML_MODEL=path vers pipeline.joblib
"""
from __future__ import annotations

import os
from pathlib import Path

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from preprocess import preprocess_for_model

MODEL_PATH = Path(os.environ.get("RECLAMATION_ML_MODEL", "artifacts/pipeline.joblib"))

app = FastAPI(title="Reclamation ML classifier", version="1.0.0")
_pipeline = None


def get_pipeline():
    global _pipeline
    if _pipeline is None:
        if not MODEL_PATH.is_file():
            raise RuntimeError(f"Model file not found: {MODEL_PATH.resolve()}")
        _pipeline = joblib.load(MODEL_PATH)
    return _pipeline


class PredictRequest(BaseModel):
    text: str = Field(..., min_length=1, description="Texte brut (ex: sujet + description)")


class PredictResponse(BaseModel):
    category: str


@app.get("/health")
def health():
    return {"status": "ok", "model": str(MODEL_PATH)}


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    try:
        pipe = get_pipeline()
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    processed = preprocess_for_model(req.text)
    if not processed.strip():
        raise HTTPException(status_code=400, detail="Texte vide après prétraitement")
    cat = pipe.predict([processed])[0]
    return PredictResponse(category=str(cat))
