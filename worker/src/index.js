/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | پراکسی معکوس هوش مصنوعی (Cloudflare Worker)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کلید Gemini هرگز در اپ اندروید ذخیره نمی‌شود؛ ورکر کلید را از
 *  Secrets می‌خواند و درخواست‌ها را به گوگل پاس می‌دهد + محدودیت نرخ.
 *  استقرار:  wrangler deploy  و  wrangler secret put GEMINI_API_KEY
 * ═══════════════════════════════════════════════════════════════════════════
 */

const UPSTREAM = 'https://generativelanguage.googleapis.com';
const ALLOWED_MODELS = ['gemini-1.5-flash', 'gemini-1.5-pro', 'gemini-2.0-flash'];

// حداکثر درخواست در دقیقه به ازای هر IP (با KV اختیاری، ورنه درون‌حافظه‌ای)
const RATE_LIMIT_PER_MIN = 20;
const memoryBuckets = new Map();

export default {
  async fetch(request, env, ctx) {
    // ── CORS (پیش‌پرواز برای کلاینت‌ها) ──────────────────────────────────
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(request.url);

    // فقط مسیر مدل‌های مجاز پروکسی می‌شوند
    const modelMatch = url.pathname.match(
      /^\/v1beta\/models\/([a-z0-9.\-]+):(generateContent|streamGenerateContent)$/
    );
    if (!modelMatch) {
      return json({ error: 'مسیر غیرمجاز است' }, 404);
    }

    const [, model, method] = modelMatch;
    if (!ALLOWED_MODELS.includes(model)) {
      return json({ error: `مدل ${model} در فهرست مجاز نیست` }, 403);
    }

    if (request.method !== 'POST') {
      return json({ error: 'فقط POST مجاز است' }, 405);
    }

    // ── محدودیت نرخ بر اساس IP ───────────────────────────────────────────
    const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
    const blocked = await rateLimited(env, ip);
    if (blocked) return json({ error: 'سقف درخواست‌ها پر شده؛ لحظاتی صبر کنید' }, 429);

    // ── پیکربندی مدل از سمت ورکر اعمال می‌شود (امنیت بیشتر) ─────────────
    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: 'بدنه JSON نامعتبر است' }, 400);
    }
    body.safetySettings = body.safetySettings ?? [
      { category: 'HARM_CATEGORY_HARASSMENT', threshold: 'BLOCK_ONLY_HIGH' },
    ];

    if (!env.GEMINI_API_KEY) {
      return json({ error: 'کلید سرور پیکربندی نشده (wrangler secret put GEMINI_API_KEY)' }, 500);
    }

    // ── ارسال به گوگل با کلید محرمانه ────────────────────────────────────
    const upstreamUrl = `${UPSTREAM}/v1beta/models/${model}:${method}?key=${encodeURIComponent(env.GEMINI_API_KEY)}`;
    const upstream = await fetch(upstreamUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    const payload = await upstream.text();
    return new Response(payload, {
      status: upstream.status,
      headers: {
        ...corsHeaders(),
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store',
      },
    });
  },
};

/** محدودسازی نرخ: ترجیحاً KV، در غیر این صورت سطل‌های درون‌حافظه‌ای. */
async function rateLimited(env, ip) {
  const windowKey = Math.floor(Date.now() / 60_000);

  if (env.RATE_LIMIT_KV) {
    const key = `rl:${ip}:${windowKey}`;
    const current = parseInt((await env.RATE_LIMIT_KV.get(key)) ?? '0', 10);
    if (current >= RATE_LIMIT_PER_MIN) return true;
    await env.RATE_LIMIT_KV.put(key, String(current + 1), { expirationTtl: 120 });
    return false;
  }

  const key = `${ip}:${windowKey}`;
  const bucket = memoryBuckets.get(key) ?? 0;
  if (bucket >= RATE_LIMIT_PER_MIN) return true;
  memoryBuckets.set(key, bucket + 1);
  // پاکسازی دوره‌ای سطل‌های قدیمی
  if (memoryBuckets.size > 10_000) memoryBuckets.clear();
  return false;
}

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, X-Api-Key',
    'Access-Control-Max-Age': '86400',
  };
}

function json(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { ...corsHeaders(), 'Content-Type': 'application/json; charset=utf-8' },
  });
}
