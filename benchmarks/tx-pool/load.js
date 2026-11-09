// One step of the load test: a fixed arrival rate (open model), so a slow app does not slow the sender down.
// RATE orders per second for DURATION, against /orders or /orders-narrowed, with SHARE of them for marketplace
// products (forwarded to the partner API, no database) and the rest for products we stock (two writes).
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
      // Enough VUs up front for most requests to wait out the 2 s pool timeout; k6 still dropped some in the most
      // overloaded steps (eager, 25% partner, 200 and 250 req/s), which bench.py counts as failures.
      preAllocatedVUs: Math.max(100, RATE * 4),
      maxVUs: 3000,
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

// The whole summary as JSON on stdout, for bench.py.
export function handleSummary(data) {
  return { stdout: JSON.stringify(data) };
}

export default function () {
  // Deterministic split: out of every 20 orders, SHARE*20 go to a partner (20 slots, so 95% is exact).
  const i = exec.scenario.iterationInTest;
  const route = (i % 20) < Math.round(SHARE * 20) ? 'downstream' : 'db';
  // Own-stock orders spread over 100 stock rows, so the decrements don't queue on one row lock.
  const product = route === 'db' ? `own-${i % 100}` : `partner-${i % 100}`;
  http.post(`${BASE}/${TARGET}?product=${product}&quantity=1`, null, { tags: { route }, timeout: '10s' });
}
