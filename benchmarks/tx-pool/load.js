// One step of the load test: a fixed arrival rate (open model), so a slow app does not slow the sender down.
// RATE requests per second for DURATION, against /mixed or /narrowed, with SHARE of them on the downstream path.
import http from 'k6/http';
import exec from 'k6/execution';

const RATE = Number(__ENV.RATE);
const SHARE = Number(__ENV.SHARE);
const TARGET = __ENV.TARGET;
const BASE = __ENV.BASE || 'http://app:8080';

export const options = {
  scenarios: {
    step: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: __ENV.DURATION || '30s',
      preAllocatedVUs: Math.max(50, RATE),
      maxVUs: 1000,
    },
  },
  // Thresholds that always pass, only so the summary carries the per-route submetrics.
  thresholds: {
    'http_req_duration{route:db}': ['max>=0'],
    'http_req_duration{route:downstream}': ['max>=0'],
    'http_req_failed{route:db}': ['rate>=0'],
    'http_req_failed{route:downstream}': ['rate>=0'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
};

export default function () {
  // Deterministic split: out of every 10 requests, SHARE*10 take the downstream path.
  const route = (exec.scenario.iterationInTest % 10) < Math.round(SHARE * 10) ? 'downstream' : 'db';
  http.post(`${BASE}/${TARGET}?route=${route}`, null, { tags: { route }, timeout: '10s' });
}
