/**
 * BestieAYG weather proxy — Cloudflare Worker.
 *
 * Why this exists:
 *   The Android app used to ship the OpenWeather API key in BuildConfig.
 *   Pentest Test 1 confirmed the key was trivially recoverable from the APK
 *   with jadx. This proxy moves the key off the device entirely.
 *
 * How it works:
 *   Android app ──HTTPS GET /weather?lat=..&lon=..──▶ this Worker
 *                                                      │
 *                                                      ▼
 *                                  https://api.openweathermap.org/...&appid=SECRET
 *                                                      │
 *   Android app ◀───────────── JSON response ──────────┘
 *
 * The OpenWeather key lives only in Cloudflare's encrypted secret store
 * (env.OPENWEATHER_API_KEY), reachable in the Worker runtime but never
 * shipped to clients.
 */

const OPENWEATHER_BASE = "https://api.openweathermap.org/data/2.5/weather";

// CORS — fine to keep wildcard; the Worker is read-only and the key never leaves.
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...CORS_HEADERS },
  });
}

function isFiniteNumber(value) {
  const n = Number(value);
  return Number.isFinite(n);
}

export default {
  async fetch(request, env) {
    // Preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    if (request.method !== "GET") {
      return jsonResponse({ error: "Method not allowed" }, 405);
    }

    const url = new URL(request.url);

    // Tiny landing page so the bare URL doesn't look broken in a browser.
    if (url.pathname === "/" || url.pathname === "") {
      return jsonResponse({
        service: "bayg-weather-proxy",
        usage: "GET /weather?lat=<lat>&lon=<lon>",
      });
    }

    if (url.pathname !== "/weather") {
      return jsonResponse({ error: "Not found" }, 404);
    }

    const lat = url.searchParams.get("lat");
    const lon = url.searchParams.get("lon");

    if (!isFiniteNumber(lat) || !isFiniteNumber(lon)) {
      return jsonResponse({ error: "lat and lon must be numbers" }, 400);
    }
    const latNum = Number(lat);
    const lonNum = Number(lon);
    if (latNum < -90 || latNum > 90 || lonNum < -180 || lonNum > 180) {
      return jsonResponse({ error: "lat/lon out of range" }, 400);
    }

    if (!env.OPENWEATHER_API_KEY) {
      return jsonResponse({ error: "Server misconfigured" }, 500);
    }

    const upstream = new URL(OPENWEATHER_BASE);
    upstream.searchParams.set("lat", String(latNum));
    upstream.searchParams.set("lon", String(lonNum));
    upstream.searchParams.set("units", "metric");
    upstream.searchParams.set("appid", env.OPENWEATHER_API_KEY);

    let upstreamResponse;
    try {
      upstreamResponse = await fetch(upstream.toString(), {
        // Edge cache: weather doesn't change in 60s; saves quota + speeds up.
        cf: { cacheTtl: 60, cacheEverything: true },
      });
    } catch (e) {
      return jsonResponse({ error: "Upstream fetch failed" }, 502);
    }

    const body = await upstreamResponse.text();
    return new Response(body, {
      status: upstreamResponse.status,
      headers: {
        "Content-Type": "application/json",
        "Cache-Control": "public, max-age=60",
        ...CORS_HEADERS,
      },
    });
  },
};
