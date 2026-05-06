import pandas as pd
import numpy as np
import joblib
import scipy.sparse as sp
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import r2_score, mean_absolute_error
from scipy.sparse import hstack, csr_matrix

print("🚀 Chargement du dataset...")
df = pd.read_csv("Salaries.csv")

# ✅ Nettoyage
df = df[["JobTitle", "BasePay", "OvertimePay",
         "TotalPay", "Year"]].copy()
df = df.dropna(subset=["TotalPay"])
df["TotalPay"]    = pd.to_numeric(df["TotalPay"],    errors="coerce")
df["BasePay"]     = pd.to_numeric(df["BasePay"],     errors="coerce").fillna(0)
df["OvertimePay"] = pd.to_numeric(df["OvertimePay"], errors="coerce").fillna(0)
df["Year"]        = df["Year"].fillna(df["Year"].median())
df["JobTitle"]    = df["JobTitle"].fillna("Unknown")

# ✅ Filtre salaires
df = df[(df["TotalPay"] >= 20000) & (df["TotalPay"] <= 400000)]

# ✅ Supprime outliers IQR
Q1  = df["TotalPay"].quantile(0.25)
Q3  = df["TotalPay"].quantile(0.75)
IQR = Q3 - Q1
df  = df[
    (df["TotalPay"] >= Q1 - 1.5 * IQR) &
    (df["TotalPay"] <= Q3 + 1.5 * IQR)
]

print(f"✅ Dataset nettoyé: {df.shape}")
print(f"💰 Salaire moyen : ${df['TotalPay'].mean():,.0f}")

# ✅ TF-IDF sur JobTitle
tfidf = TfidfVectorizer(max_features=500, ngram_range=(1, 2))
X_job = tfidf.fit_transform(df["JobTitle"])

# ✅ Features numériques
X_num = csr_matrix(df[["BasePay", "OvertimePay", "Year"]].values)

# ✅ Combine
X = hstack([X_job, X_num])
y = df["TotalPay"]

# ✅ Split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# ✅ Scaler
X_train_dense = X_train.toarray()
X_test_dense  = X_test.toarray()
scaler        = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train_dense)
X_test_scaled  = scaler.transform(X_test_dense)

# ✅ Entraîne le meilleur modèle
print("🤖 Entraînement Gradient Boosting...")
model = GradientBoostingRegressor(
    n_estimators=100,
    learning_rate=0.1,
    max_depth=5,
    random_state=42
)
model.fit(X_train_scaled, y_train)

# ✅ Évalue
y_pred = model.predict(X_test_scaled)
r2  = r2_score(y_test, y_pred)
mae = mean_absolute_error(y_test, y_pred)
print(f"✅ R²  = {r2:.4f}")
print(f"✅ MAE = ${mae:,.0f}")

# ✅ Sauvegarde
joblib.dump(model,  "model.pkl")
joblib.dump(tfidf,  "tfidf.pkl")
joblib.dump(scaler, "scaler.pkl")

# ✅ Sauvegarde stats pour conversion USD→TND
stats = {
    "mean_salary": float(df["TotalPay"].mean()),
    "min_salary":  float(df["TotalPay"].min()),
    "max_salary":  float(df["TotalPay"].max()),
    "mean_base":   float(df["BasePay"].mean()),
    "mean_ot":     float(df["OvertimePay"].mean()),
    "mean_year":   float(df["Year"].mean())
}
joblib.dump(stats, "stats.pkl")

print("\n🎉 Modèle sauvegardé !")
print("   model.pkl  ✅")
print("   tfidf.pkl  ✅")
print("   scaler.pkl ✅")
print("   stats.pkl  ✅")