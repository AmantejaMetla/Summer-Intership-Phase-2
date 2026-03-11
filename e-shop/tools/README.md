# Tools

## CSV → product-seed.json

After you **download** the Kaggle or Maven coffee-shop dataset (CSV/Excel), you can turn it into our seed format.

1. If you have **Excel**, export or save as **CSV** (e.g. `coffee-shop-products.csv`).
2. Open `csv-to-product-seed.js` and check **HEADER_MAP** – it maps common column names (Product, Category, Unit Price, etc.) to our fields. If your CSV uses different headers, add them to the arrays (e.g. `name: ['Product', 'product_name', 'Item']`).
3. Run (from project root):

   ```bash
   node tools/csv-to-product-seed.js path\to\your\coffee-shop.csv
   ```

4. This creates **tools/product-seed-generated.json**. Copy it to **product-service/src/main/resources/product-seed.json** (replace the existing file if you want).
5. **Images:** The script sets a placeholder `imageUrl` per product. Replace those with real image URLs from Unsplash/Pexels if you like (edit the JSON or update via API later).

**Test with sample:**  
`node tools/csv-to-product-seed.js tools/sample-products.csv`
