/**
 * Converts a CSV (e.g. from Kaggle/Maven coffee shop dataset) into product-seed.json format.
 * Run: node tools/csv-to-product-seed.js [path-to-your.csv]
 * Output: product-seed.json in the same folder as the script (copy to product-service/src/main/resources/)
 *
 * CSV should have a header row. Map your column names below (HEADER_MAP).
 * If your CSV uses different names, change HEADER_MAP to match.
 * imageUrl is not in the dataset - we use a placeholder; replace later in the JSON or in DB.
 */

const fs = require('fs');
const path = require('path');

// Map your CSV column headers to our seed fields (case-insensitive match)
const HEADER_MAP = {
  name: ['product', 'product_name', 'product name', 'item', 'name', 'Product'],
  categoryName: ['category', 'product_type', 'product type', 'type', 'Category'],
  price: ['unit_price', 'unit price', 'price', 'retail_price', 'Price'],
  description: ['description', 'desc', 'Description'],
  stockQuantity: ['stock', 'quantity', 'inventory', 'Stock', 'Quantity'],
};

const PLACEHOLDER_IMAGE = 'https://picsum.photos/seed/'; // we append index for variety

function findColumn(row, possibleNames) {
  const lower = row.map((c) => (c || '').trim().toLowerCase());
  for (const name of possibleNames) {
    const idx = lower.indexOf(name.toLowerCase());
    if (idx >= 0) return idx;
  }
  return -1;
}

function parseCSV(content) {
  const lines = content.split(/\r?\n/).filter((l) => l.trim());
  if (lines.length < 2) return [];
  const header = lines[0].split(',').map((c) => c.trim());
  const rows = [];
  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',').map((c) => c.trim());
    rows.push(values);
  }
  return { header, rows };
}

function main() {
  const csvPath = process.argv[2] || path.join(__dirname, 'sample-products.csv');
  if (!fs.existsSync(csvPath)) {
    console.error('Usage: node csv-to-product-seed.js <path-to-your.csv>');
    console.error('Example: node csv-to-product-seed.js C:\\Downloads\\coffee-shop-sales.csv');
    console.error('No file at:', csvPath);
    process.exit(1);
  }
  const content = fs.readFileSync(csvPath, 'utf8');
  const { header, rows } = parseCSV(content);

  const nameIdx = findColumn(header, HEADER_MAP.name);
  const categoryIdx = findColumn(header, HEADER_MAP.categoryName);
  const priceIdx = findColumn(header, HEADER_MAP.price);
  const descIdx = findColumn(header, HEADER_MAP.description);
  const stockIdx = findColumn(header, HEADER_MAP.stockQuantity);

  if (nameIdx < 0) {
    console.error('Could not find a "name" column. Your CSV headers:', header);
    process.exit(1);
  }

  const seen = new Set();
  const products = [];
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const name = (row[nameIdx] || '').trim();
    if (!name || seen.has(name)) continue;
    seen.add(name);
    const categoryName = (categoryIdx >= 0 ? row[categoryIdx] : '')?.trim() || 'General';
    let price = 0;
    if (priceIdx >= 0 && row[priceIdx]) {
      const p = parseFloat(String(row[priceIdx]).replace(/[^0-9.]/g, ''));
      if (!isNaN(p)) price = p;
    }
    const description = (descIdx >= 0 ? row[descIdx] : '')?.trim() || '';
    let stockQuantity = 10;
    if (stockIdx >= 0 && row[stockIdx]) {
      const s = parseInt(String(row[stockIdx]).replace(/\D/g, ''), 10);
      if (!isNaN(s)) stockQuantity = s;
    }
    products.push({
      name,
      description: description || name,
      price: Math.round(price * 100) / 100,
      categoryName,
      stockQuantity,
      imageUrl: PLACEHOLDER_IMAGE + (i + 1) + '/400/400',
    });
  }

  const outPath = path.join(__dirname, 'product-seed-generated.json');
  fs.writeFileSync(outPath, JSON.stringify(products, null, 2), 'utf8');
  console.log('Wrote', products.length, 'products to', outPath);
  console.log('Copy to: product-service/src/main/resources/product-seed.json');
  console.log('Then replace imageUrl values with real image URLs (Unsplash/Pexels) if you like.');
}

main();
