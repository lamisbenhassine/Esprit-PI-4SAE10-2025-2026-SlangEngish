# Déploiement Kubernetes depuis WSL

Deux contextes courants : **kubectl vers le cluster intégré de Docker Desktop (Windows)** ou **cluster créé avec kubeadm** sur une machine Linux.

## 1. Prérequis

- Images disponibles pour le nœud Kubernetes : `lamisbenhassine/user-service:latest` et `lamisbenhassine/reclamation-service:latest` (build + push Docker Hub, ou `docker save` / import selon ton cluster).
- **Secret MySQL** : copier `mysql-secret.yaml.example` vers `mysql-secret.yaml`, remplacer `CHANGE_ME`, ne pas committer le fichier rempli.

## 2. WSL + kubectl vers Docker Desktop (recommandé pour un PC unique)

1. Sous **Windows** : Docker Desktop → **Settings** → **Kubernetes** → activer Kubernetes, attendre que le cluster soit *running*.
2. Sous **WSL** (Ubuntu, etc.) :
   - Installer kubectl : suivre [Install kubectl on Linux](https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/).
   - Réutiliser le kubeconfig Windows (adapter le nom d’utilisateur) :

```bash
export KUBECONFIG="/mnt/c/Users/VOTRE_USER_WINDOWS/.kube/config"
kubectl cluster-info
```

3. Depuis le repo (chemin WSL vers le projet), appliquer les manifests **dans l’ordre** :

```bash
cd /mnt/c/Users/VOTRE_USER/Desktop/.../SlangEngish/PID/k8s
kubectl apply -f 00-namespace.yaml
kubectl apply -f mysql-secret.yaml          # votre copie locale, pas le .example
kubectl apply -f 02-mysql-configmap.yaml
kubectl apply -f 03-mysql.yaml
# Attendre que MySQL soit prêt (premier démarrage + init SQL peut prendre 1–2 min)
kubectl wait --for=condition=ready pod -l app=mysql -n slangenglish --timeout=180s
kubectl apply -f 04-user-service.yaml
kubectl apply -f 05-reclamation-service.yaml
```

4. Accès aux APIs depuis ta machine :

```bash
kubectl port-forward -n slangenglish svc/user-service 8011:8011
# autre terminal
kubectl port-forward -n slangenglish svc/reclamation-service 8012:8012
```

Puis `http://localhost:8011` et `http://localhost:8012`.

## 3. Cluster avec **kubeadm** (VM / serveur Linux)

- kubeadm s’installe sur **Linux** (souvent une VM ou un serveur), pas typiquement “dans WSL” comme nœud unique de prod.
- Étapes officielles : [Creating a cluster with kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/create-cluster-kubeadm/).
- Sur la machine où tu as exécuté `kubeadm init`, récupère `/etc/kubernetes/admin.conf`, copie-le sur ton PC (ou dans WSL) et :

```bash
export KUBECONFIG=/chemin/vers/admin.conf
kubectl get nodes
```

- Il faudra un **CNI** (Calico, Flannel, etc.) et souvent retirer le *taint* sur le nœud control-plane si tu n’as qu’un seul nœud pour y planifier les pods.
- Charge ensuite les **mêmes YAML** avec `kubectl apply` comme ci-dessus (les images doivent être **pullables** depuis le cluster, donc souvent **Docker Hub** avec `imagePullPolicy: Always` si tu ne peux pas charger les images localement).

## 4. PVC MySQL

Si le `PersistentVolumeClaim` reste en `Pending`, vérifie qu’un **StorageClass** par défaut existe (`kubectl get sc`). Sur Docker Desktop il y en a souvent un. Sinon, pour un lab uniquement, tu peux remplacer le volume du Deployment MySQL par un `emptyDir` (données perdues au redémarrage du pod).

## 5. Eureka

Les manifests désactivent Eureka (`EUREKA_CLIENT_ENABLED=false`) pour un déploiement **sans** serveur Eureka dans le cluster. Si tu ajoutes un pod Eureka plus tard, retire ces variables et configure `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
