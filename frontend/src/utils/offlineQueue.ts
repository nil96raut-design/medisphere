export interface QueuedRequest {
  id: string;
  url: string;
  method: string;
  body?: any;
  timestamp: number;
}

const STORAGE_KEY = 'healthtrack_offline_queue';

export const enqueueRequest = (url: string, method: string, body?: any) => {
  const queue: QueuedRequest[] = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
  const newReq: QueuedRequest = {
    id: `req_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
    url,
    method,
    body,
    timestamp: Date.now(),
  };
  queue.push(newReq);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
  console.log('[OfflineQueue] Enqueued request for retry when online:', newReq);
};

export const processOfflineQueue = async () => {
  const queue: QueuedRequest[] = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
  if (queue.length === 0) return;

  console.log(`[OfflineQueue] Processing ${queue.length} offline queued requests...`);
  const remaining: QueuedRequest[] = [];

  for (const req of queue) {
    try {
      const response = await fetch(req.url, {
        method: req.method,
        headers: { 'Content-Type': 'application/json' },
        body: req.body ? JSON.stringify(req.body) : undefined,
      });

      if (!response.ok) {
        remaining.push(req);
      } else {
        console.log(`[OfflineQueue] Successfully replayed offline request: ${req.url}`);
      }
    } catch (err) {
      console.warn(`[OfflineQueue] Failed to replay request ${req.url}, keeping in queue:`, err);
      remaining.push(req);
    }
  }

  localStorage.setItem(STORAGE_KEY, JSON.stringify(remaining));
};

if (typeof window !== 'undefined') {
  window.addEventListener('online', () => {
    processOfflineQueue();
  });
}
