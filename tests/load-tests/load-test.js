import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
const accounts = new SharedArray('accounts', function () {
    return ["load", "load2", "load3", "load4", "load5"];
});

export const options = {
    scenarios: {
        free_tier_safe: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: 100,
            stages: [
                { target: 10, duration: '1m' }, // Warm up to 10 RPS
                { target: 30, duration: '5m' }, // Slightly higher than expected DynamoDB free tier limit
                { target: 0, duration: '1m' },  // Ramp down
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'], // Allow 5% for cold starts/initial throttling
        'http_req_duration{expected_response:true}': ['p(95)<500'], // Expecting fast responses
    },
};

/** REPLACE WITH ACTUAL API GATEWAY HTTP API URL **/
const BASE_URL = 'https://zxfccwjjn5.execute-api.us-east-1.amazonaws.com/Prod';

export default function () {
    const accountId = accounts[Math.floor(Math.random() * accounts.length)];
    const realCardId = `REAL#${accountId}`;

    const rand = Math.random();

    let res;
    if (rand < 0.6) {
        res = http.get(`${BASE_URL}/cards/real?accountId=${accountId}`);
        check(res, {
            'real card status 200': (r) => r.status === 200,
        });
    } else if (rand < 0.9) {
        const payload = JSON.stringify({
            realCardId: realCardId,
            limit: 500,
        });
        const params = {
            headers: { 'Content-Type': 'application/json' },
        };
        // res = http.post(`${BASE_URL}/cards/vcn`, payload, params);
        res = http.post(`${BASE_URL}/cards/vcn?realCardId=${encodeURIComponent(realCardId)}&limit=500`, null, params);
        check(res, {
            'issue vcn status 200/201': (r) => r.status === 200 || r.status === 201,
        });
    } else {
        res = http.get(`${BASE_URL}/cards/vcns?realCardId=${realCardId}`);
        check(res, {
            'list vcns status 200': (r) => r.status === 200,
        });
    }
}