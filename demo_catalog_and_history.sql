-- OPTIONAL DEMONSTRATION DATA for PricePath; MySQL 8.0+
-- Does NOT drop, reset, or rename any table. Uses existing Sellers.
-- Sample prices and history are illustrative, NOT live retailer prices.
USE pricepath_new;

INSERT INTO Products (Name, Category, Brand, Description)
SELECT v.Name, v.Category, v.Brand, v.Description
FROM (
 SELECT 'Levi''s 511 Slim Jeans' Name,'Clothing' Category,'Levi''s' Brand,'Slim-fit denim jeans; sample listing.' Description UNION ALL
 SELECT 'H&M Cotton Crew T-Shirt','Clothing','H&M','Cotton crew-neck T-shirt; sample listing.' UNION ALL
 SELECT 'Puma Everyday Sneakers','Clothing','Puma','Casual everyday sneakers; sample listing.' UNION ALL
 SELECT 'Adidas Training Track Pants','Clothing','Adidas','Training track pants; sample listing.' UNION ALL
 SELECT 'Tata Sampann Toor Dal 1kg','Food','Tata Sampann','One-kilogram pack of toor dal; sample listing.' UNION ALL
 SELECT 'Aashirvaad Atta 5kg','Food','Aashirvaad','Five-kilogram whole-wheat atta; sample listing.' UNION ALL
 SELECT 'Tata Tea Gold 500g','Food','Tata Tea','500g tea pack; sample listing.' UNION ALL
 SELECT 'Amul Butter 500g','Food','Amul','500g butter pack; sample listing.' UNION ALL
 SELECT 'IKEA Table Lamp','Home Decor','IKEA','Table lamp for indoor use; sample listing.' UNION ALL
 SELECT 'Home Centre Cushion Cover','Home Decor','Home Centre','Decorative cushion cover; sample listing.' UNION ALL
 SELECT 'Solimo Bedsheet Double','Home Decor','Amazon Solimo','Double bedsheet; sample listing.' UNION ALL
 SELECT 'Milton Stainless Steel Bottle','Home & Kitchen','Milton','Reusable stainless-steel bottle; sample listing.' UNION ALL
 SELECT '3M Car Phone Mount','Vehicle Accessories','3M','Dashboard phone mount; sample listing.' UNION ALL
 SELECT 'Bosch Car Wiper Blades','Vehicle Accessories','Bosch','Replacement car wiper blades; verify vehicle fitment.' UNION ALL
 SELECT 'Michelin Digital Tyre Gauge','Vehicle Accessories','Michelin','Digital tyre pressure gauge; sample listing.' UNION ALL
 SELECT 'Ambrane 10000mAh Power Bank','Electronics','Ambrane','10,000mAh portable power bank; sample listing.' UNION ALL
 SELECT 'JBL Go 3 Bluetooth Speaker','Electronics','JBL','Portable Bluetooth speaker; sample listing.' UNION ALL
 SELECT 'SanDisk Ultra 128GB USB Drive','Electronics','SanDisk','128GB USB storage drive; sample listing.' UNION ALL
 SELECT 'Logitech K380 Keyboard','Accessories','Logitech','Compact wireless keyboard; sample listing.' UNION ALL
 SELECT 'Philips LED Bulb 9W','Home & Kitchen','Philips','9W LED bulb; sample listing.'
) v
WHERE NOT EXISTS (SELECT 1 FROM Products p WHERE p.Name=v.Name AND p.Brand=v.Brand);

-- Add sample offers for each newly listed product using currently existing sellers.
-- If fewer than three sellers exist, it simply creates fewer offers.
INSERT INTO SellerProducts (ProductID,SellerID,Price,Discount,DeliveryCharge,PlatformFee,DeliveryDays,StockStatus)
SELECT p.ProductID,s.SellerID,
       CASE s.SellerName WHEN 'Amazon' THEN 799.00 WHEN 'Flipkart' THEN 829.00 WHEN 'Croma' THEN 849.00 ELSE 819.00 END,
       CASE s.SellerName WHEN 'Amazon' THEN 50.00 WHEN 'Flipkart' THEN 60.00 WHEN 'Croma' THEN 40.00 ELSE 45.00 END,
       CASE s.SellerName WHEN 'Flipkart' THEN 40.00 ELSE 0.00 END,
       CASE s.SellerName WHEN 'Croma' THEN 20.00 ELSE 0.00 END,
       CASE s.SellerName WHEN 'Amazon' THEN 2 WHEN 'Flipkart' THEN 3 ELSE 2 END,
       'in_stock'
FROM Products p CROSS JOIN Sellers s
WHERE p.Name IN ('Levi''s 511 Slim Jeans','H&M Cotton Crew T-Shirt','Puma Everyday Sneakers','Adidas Training Track Pants','Tata Sampann Toor Dal 1kg','Aashirvaad Atta 5kg','Tata Tea Gold 500g','Amul Butter 500g','IKEA Table Lamp','Home Centre Cushion Cover','Solimo Bedsheet Double','Milton Stainless Steel Bottle','3M Car Phone Mount','Bosch Car Wiper Blades','Michelin Digital Tyre Gauge','Ambrane 10000mAh Power Bank','JBL Go 3 Bluetooth Speaker','SanDisk Ultra 128GB USB Drive','Logitech K380 Keyboard','Philips LED Bulb 9W')
AND NOT EXISTS (SELECT 1 FROM SellerProducts sp WHERE sp.ProductID=p.ProductID AND sp.SellerID=s.SellerID);

-- Optional illustrative history: three historical snapshots per existing offer.
-- This is synthetic demo data so the graph can be demonstrated; do not present as real historical prices.
INSERT INTO PriceHistory (SellerProductID,Price,Discount,DeliveryCharge,PlatformFee,RecordedAt)
SELECT sp.SellerProductID,
       ROUND(sp.Price * x.multiplier,2),
       ROUND(sp.Discount * x.multiplier,2),
       sp.DeliveryCharge, sp.PlatformFee,
       DATE_SUB(NOW(), INTERVAL x.days_ago DAY)
FROM SellerProducts sp
CROSS JOIN (SELECT 90 days_ago, 1.08 multiplier UNION ALL SELECT 30,1.04 UNION ALL SELECT 7,1.02) x
WHERE NOT EXISTS (
 SELECT 1 FROM PriceHistory ph WHERE ph.SellerProductID=sp.SellerProductID
 AND DATE(ph.RecordedAt)=DATE(DATE_SUB(NOW(), INTERVAL x.days_ago DAY))
);

-- Verify counts after running this optional script.
SELECT COUNT(*) AS products FROM Products;
SELECT COUNT(*) AS sellers FROM Sellers;
SELECT COUNT(*) AS offers FROM SellerProducts;
SELECT COUNT(*) AS history_records FROM PriceHistory;
