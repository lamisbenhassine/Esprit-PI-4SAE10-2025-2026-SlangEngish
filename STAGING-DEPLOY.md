# Staging Deployment (Inscription + Forum)

## 1) Prepare environment file

Create `.env.staging` from the example:

```bash
cp .env.staging.example .env.staging
```

Fill at least:

- `DOCKERHUB_USERNAME`
- `INSCRIPTION_IMAGE_TAG`
- `FORUM_IMAGE_TAG`
- database passwords

## 2) Login to Docker Hub

```bash
echo <YOUR_DOCKERHUB_TOKEN> | docker login -u <YOUR_DOCKERHUB_USERNAME> --password-stdin
```

The token must have image pull permission.

## 3) Start staging stack

```bash
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d
```

## 4) Check status

```bash
docker compose --env-file .env.staging -f docker-compose.staging.yml ps
docker compose --env-file .env.staging -f docker-compose.staging.yml logs -f inscription forum
```

## 5) Stop stack

```bash
docker compose --env-file .env.staging -f docker-compose.staging.yml down
```
