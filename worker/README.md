# Developed by Milano Technical Team, Milad Yaghoobi

## استقرار پراکسی هوش مصنوعی روی Cloudflare Workers

```bash
cd worker
npm install -g wrangler
wrangler login

# ۱) ثبت کلید گوگل به‌صورت امن (هرگز در کد یا اپ قرار ندهید)
wrangler secret put GEMINI_API_KEY

# ۲) استقرار
wrangler deploy
```

خروجی یک آدرس مشابه زیر می‌دهد:

```
https://vizitor-ai-proxy.YOUR-SUBDOMAIN.workers.dev
```

این آدرس را در اپ: **تب گزارشات ← پیکربندی سرور ← «آدرس پراکسی هوش مصنوعی»** وارد کنید.

### محدودیت نرخ دقیق‌تر (اختیاری)
```bash
wrangler kv:namespace create RATE_LIMIT_KV
```
سپس `id` را در `wrangler.toml` جایگذاری و دوباره `wrangler deploy` کنید.

### نکته امنیتی
- کلید `GEMINI_API_KEY` فقط در Secrets ورکر زندگی می‌کند.
- فقط مدل‌های فهرست `ALLOWED_MODELS` و متد `POST` پروکسی می‌شوند.
- هر IP حداکثر ۲۰ درخواست در دقیقه مجاز است.
