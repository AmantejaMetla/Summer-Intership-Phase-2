/**
 * Reads dataset/kaggle/items.csv and orders.csv, generates init-db/07-seed-orders-from-dataset.sql
 * Run from project root: node tools/generate-orders-seed.js
 */
const fs = require('fs');
const path = require('path');

const projectRoot = path.join(__dirname, '..');
const itemsPath = path.join(projectRoot, 'dataset', 'kaggle', 'items.csv');
const ordersPath = path.join(projectRoot, 'dataset', 'kaggle', 'orders.csv');
const outPath = path.join(projectRoot, 'init-db', '07-seed-orders-from-dataset.sql');

// item_id (It001..It024) -> { productId: 1..24, price }
function parseItems(content) {
  const lines = content.split(/\r?\n/).filter((l) => l.trim());
  const header = lines[0].split(',');
  const nameIdx = header.findIndex((h) => h.trim() === 'item_id');
  const priceIdx = header.findIndex((h) => h.trim() === 'item_price');
  const map = {};
  for (let i = 1; i < lines.length; i++) {
    const row = lines[i].split(',');
    const itemId = (row[nameIdx] || '').trim();
    const price = parseFloat((row[priceIdx] || '0').replace(/"/g, '')) || 0;
    const num = parseInt(itemId.replace(/\D/g, ''), 10);
    if (itemId && !isNaN(num) && num >= 1 && num <= 24) {
      map[itemId] = { productId: num, price };
    }
  }
  map['It0010'] = map['It010'] || { productId: 10, price: 4.60 };
  return map;
}

function parseOrders(content, itemMap) {
  const lines = content.split(/\r?\n/).filter((l) => l.trim());
  const header = lines[0].split(',');
  const orderIdIdx = header.findIndex((h) => h.trim() === 'order_id');
  const createdAtIdx = header.findIndex((h) => h.trim() === 'created_at');
  const itemIdIdx = header.findIndex((h) => h.trim() === 'item_id');
  const qtyIdx = header.findIndex((h) => h.trim() === 'quantity');

  const orderRows = {};
  for (let i = 1; i < lines.length; i++) {
    const row = lines[i].split(',');
    const orderId = (row[orderIdIdx] || '').trim();
    let created_at = (row[createdAtIdx] || '').trim().replace(/"/g, '');
    const itemId = (row[itemIdIdx] || '').trim();
    const qty = parseInt((row[qtyIdx] || '1').trim(), 10) || 1;
    const info = itemMap[itemId];
    if (!info) continue;
    if (!orderRows[orderId]) {
      orderRows[orderId] = { created_at, items: [] };
    }
    if (!orderRows[orderId].created_at) orderRows[orderId].created_at = created_at;
    orderRows[orderId].items.push({ productId: info.productId, quantity: qty, price: info.price });
  }

  const orderIds = [];
  const seen = new Set();
  for (let i = 1; i < lines.length; i++) {
    const row = lines[i].split(',');
    const orderId = (row[orderIdIdx] || '').trim();
    if (orderId && !seen.has(orderId)) {
      seen.add(orderId);
      orderIds.push(orderId);
    }
  }
  const orderIdsWithItems = orderIds.filter((oid) => orderRows[oid] && orderRows[oid].items.length > 0);

  const orders = orderIdsWithItems.map((oid, idx) => {
    const data = orderRows[oid];
    const total = data.items.reduce((s, it) => s + it.quantity * it.price, 0);
    return {
      id: idx + 1,
      order_id: oid,
      created_at: data.created_at,
      total_amount: Math.round(total * 100) / 100,
      items: data.items,
    };
  });
  return orders;
}

function escapeSql(s) {
  if (s == null) return 'NULL';
  return "'" + String(s).replace(/'/g, "''") + "'";
}

function main() {
  const itemsCsv = fs.readFileSync(itemsPath, 'utf8');
  const ordersCsv = fs.readFileSync(ordersPath, 'utf8');
  const itemMap = parseItems(itemsCsv);
  const orders = parseOrders(ordersCsv, itemMap);

  let sql = `-- =============================================================================
-- Seed orders and order_items from Kaggle orders.csv (dataset).
-- Run AFTER 01-single-database-all-tables.sql and 06-seed-products-from-dataset.sql.
-- Requires a user with id=1 (seed user inserted below if not exists).
-- =============================================================================

USE eshop;

-- Seed user for order ownership (password: password). Skip if you already have user_id=1.
INSERT INTO user_credentials (id, email, password_hash) VALUES (1, 'seed@eshop.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy')
ON DUPLICATE KEY UPDATE email = email;

-- Orders (id, user_id, total_amount, status, created_at)
`;
  const orderValues = orders
    .map(
      (o) =>
        `(${o.id}, 1, ${o.total_amount}, 'CONFIRMED', ${escapeSql(o.created_at)})`
    )
    .join(',\n');
  sql += `INSERT INTO orders (id, user_id, total_amount, status, created_at) VALUES\n${orderValues};\n\n`;

  sql += `-- Order items (order_id, product_id, quantity, price)\n`;
  const itemRows = [];
  orders.forEach((o) => {
    o.items.forEach((it) => {
      itemRows.push(`(${o.id}, ${it.productId}, ${it.quantity}, ${it.price})`);
    });
  });
  sql += `INSERT INTO order_items (order_id, product_id, quantity, price) VALUES\n${itemRows.join(',\n')};\n`;

  fs.writeFileSync(outPath, sql, 'utf8');
  console.log('Wrote', orders.length, 'orders and', itemRows.length, 'order_items to', outPath);
}

main();
