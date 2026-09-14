# Frontend SRM

React + TypeScript + Vite.

## Executar

```sh
npm install
cp .env.example .env
npm run dev
```

No PowerShell, use `Copy-Item .env.example .env` para copiar o arquivo.

Para desenvolvimento local, use `VITE_API_BASE_URL=/`. O Vite encaminha chamadas
para `/api` ao backend em `http://localhost:8080`, evitando bloqueios de CORS no navegador.

Para apontar para uma API já configurada com CORS, defina `VITE_API_BASE_URL` com
a URL completa, sem barra no final.

## Verificar

```sh
npm run build
npm run lint
```
