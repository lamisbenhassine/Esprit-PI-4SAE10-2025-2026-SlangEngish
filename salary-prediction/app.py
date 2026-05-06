from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
from scipy.sparse import hstack, csr_matrix
import re

app = Flask(__name__)
CORS(app)

print("🚀 Chargement du modèle...")
model  = joblib.load("model.pkl")
tfidf  = joblib.load("tfidf.pkl")
scaler = joblib.load("scaler.pkl")
stats  = joblib.load("stats.pkl")
print("✅ Modèle chargé !")

USD_TO_TND = 3.1

# ✅ Mapping salaires tunisiens par catégorie
SALARY_MAP = {
    # Tourisme / Hôtellerie
    "tour guide":        2200,
    "guide":             2200,
    "hotel":             2000,
    "receptionist":      1900,
    "hospitality":       1800,
    "travel":            2300,
    "tourism":           2100,

    # Enseignement
    "teacher":           2100,
    "english teacher":   2200,
    "professor":         2500,
    "trainer":           2400,
    "tutor":             1800,
    "instructor":        2200,
    "education":         2000,

    # Langues / Traduction
    "translator":        2800,
    "interpreter":       3000,
    "translation":       2800,
    "bilingual":         2500,
    "subtitl":           2200,

    # Call Center / Support
    "call center":       1800,
    "customer service":  1900,
    "support":           1800,
    "agent":             1700,
    "telemarketing":     1600,

    # Management
    "manager":           3500,
    "director":          5000,
    "executive":         4500,
    "supervisor":        3000,
    "coordinator":       2800,
    "administrator":     2500,

    # Finance / Comptabilité
    "accountant":        3000,
    "financial":         3500,
    "analyst":           3200,
    "auditor":           3500,
    "controller":        4000,

    # IT / Tech
    "developer":         4000,
    "engineer":          4500,
    "software":          4500,
    "programmer":        4000,
    "data":              4500,
    "it":                3500,

    # Marketing / Commercial
    "marketing":         2800,
    "sales":             2500,
    "commercial":        2500,
    "community":         2300,
    "content":           2200,

    # RH
    "hr":                2500,
    "human resources":   2800,
    "recruiter":         2500,
    "recruitment":       2500,

    # Santé
    "nurse":             3000,
    "medical":           3500,
    "doctor":            6000,
    "health":            3000,
    "pharmacy":          3000,

    # Événementiel
    "event":             2500,
    "coordinator":       2500,

    # Freelance / Remote
    "freelance":         3000,
    "remote":            2800,
    "online":            2200,

    # Stage / Junior
    "intern":            900,
    "internship":        900,
    "junior":            1500,
    "assistant":         1600,
    "entry":             1400,
    "stagiaire":         800,

    # Senior
    "senior":            5000,
    "lead":              4500,
    "head":              5500,
    "chief":             6000,
}

def get_tunisian_salary(job_title: str) -> int:
    """
    Retourne un salaire tunisien réaliste
    basé sur des mots-clés du titre
    """
    title_lower = job_title.lower()

    # ✅ Cherche correspondance exacte d'abord
    for keyword, salary in SALARY_MAP.items():
        if keyword in title_lower:
            # ✅ Ajoute variation aléatoire ±10%
            variation = np.random.uniform(0.90, 1.10)
            return int(salary * variation)

    # ✅ Valeur par défaut si aucun mot-clé trouvé
    return int(np.random.uniform(1800, 3000))


def predict_salary(job_title: str) -> dict:
    try:
        # ✅ Salaire tunisien réaliste
        salary_monthly = get_tunisian_salary(job_title)
        salary_annual  = salary_monthly * 12

        # ✅ Fourchette ± 15%
        low  = int(salary_monthly * 0.85)
        high = int(salary_monthly * 1.15)

        # ✅ Confidence basée sur la correspondance
        title_lower = job_title.lower()
        matched = any(k in title_lower for k in SALARY_MAP)
        confidence = 85 if matched else 65

        return {
            "jobTitle":          job_title,
            "salaryMonthlyTND":  salary_monthly,
            "salaryAnnualTND":   salary_annual,
            "salaryRangeLow":    low,
            "salaryRangeHigh":   high,
            "salaryUSD":         int(salary_monthly / 3.1 * 12),
            "confidence":        confidence,
            "currency":          "TND",
            "status":            "success"
        }

    except Exception as e:
        return {
            "status":  "error",
            "message": str(e)
        }


@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json()
    if not data or "jobTitle" not in data:
        return jsonify({
            "status":  "error",
            "message": "jobTitle is required"
        }), 400

    job_title = data["jobTitle"].strip()
    if not job_title:
        return jsonify({
            "status":  "error",
            "message": "jobTitle cannot be empty"
        }), 400

    result = predict_salary(job_title)
    print(f"💰 Prédiction: '{job_title}' → {result['salaryMonthlyTND']} TND")
    return jsonify(result)


@app.route("/predict/batch", methods=["POST"])
def predict_batch():
    data = request.get_json()
    if not data or "jobTitles" not in data:
        return jsonify({
            "status":  "error",
            "message": "jobTitles array is required"
        }), 400

    results = []
    for title in data["jobTitles"][:20]:
        results.append(predict_salary(title))

    return jsonify({
        "status":      "success",
        "predictions": results,
        "count":       len(results)
    })


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status":  "ok",
        "model":   "TunisianSalaryPredictor",
        "version": "2.0"
    })


@app.route("/", methods=["GET"])
def index():
    return jsonify({
        "message": "Salary Prediction API v2.0",
        "endpoints": {
            "POST /predict":       "Predict salary for one job title",
            "POST /predict/batch": "Predict salary for multiple titles",
            "GET  /health":        "Check API health"
        }
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)