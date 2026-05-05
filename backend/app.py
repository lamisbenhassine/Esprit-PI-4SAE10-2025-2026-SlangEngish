from flask import Flask, request, jsonify
from flask_cors import CORS
import pickle
import numpy as np
import json

app = Flask(__name__)
CORS(app)  # Autoriser les requêtes depuis le frontend

# ── Charger le modèle et le scaler au démarrage ──
with open('model/kmeans_model.pkl', 'rb') as f:
    kmeans = pickle.load(f)

with open('model/scaler.pkl', 'rb') as f:
    scaler = pickle.load(f)

with open('model/cluster_profiles.json') as f:
    cluster_profiles = json.load(f)

# Labels lisibles pour chaque cluster (à adapter selon tes résultats)
CLUSTER_LABELS = {
    0: {"nom": "Apprenant Passif",    "couleur": "#E76F51", "emoji": "😴"},
    1: {"nom": "Apprenant Régulier",  "couleur": "#F4A261", "emoji": "📚"},
    2: {"nom": "Apprenant Engagé",    "couleur": "#2A9D8F", "emoji": "🚀"},
    3: {"nom": "Apprenant Excellent", "couleur": "#264653", "emoji": "🏆"},
}

FEATURES = ['SessionDuration', 'SessionsPerWeek', 'QuizScores',
            'UserSatisfaction', 'days_since_active', 'CourseCompletion']

@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()

        # Extraire les features dans le bon ordre
        values = [float(data[f]) for f in FEATURES]
        X = np.array(values).reshape(1, -1)

        # Standardiser et prédire
        X_scaled = scaler.transform(X)
        cluster = int(kmeans.predict(X_scaled)[0])

        # Distance aux centroïdes (confiance)
        distances = kmeans.transform(X_scaled)[0]
        confidence = round(1 / (1 + distances[cluster]), 4)

        label_info = CLUSTER_LABELS.get(cluster, {"nom": f"Cluster {cluster}",
                                                  "couleur": "#888",
                                                  "emoji": "❓"})

        return jsonify({
            "success": True,
            "cluster": cluster,
            "label": label_info["nom"],
            "couleur": label_info["couleur"],
            "emoji": label_info["emoji"],
            "confidence": confidence,
            "distances": distances.tolist()
        })

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400


@app.route('/profiles', methods=['GET'])
def profiles():
    return jsonify(cluster_profiles)


@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "clusters": kmeans.n_clusters})


if __name__ == '__main__':
    app.run(debug=True, port=5000)