-- Run after applying V1 and V2.
SELECT 'table_count' check_name, COUNT(*) actual_value, 29 expected_value, IF(COUNT(*)=29,'PASS','FAIL') result FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';
SELECT 'foreign_key_count' check_name, COUNT(*) actual_value, 32 expected_value, IF(COUNT(*)=32,'PASS','FAIL') result FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE();
SELECT 'roles' check_name, COUNT(*) actual_value, IF(COUNT(*)>=4,'PASS','FAIL') result FROM roles;
SELECT 'permissions' check_name, COUNT(*) actual_value, IF(COUNT(*)>=15,'PASS','FAIL') result FROM permissions;
SELECT 'demo_products' check_name, COUNT(*) actual_value, IF(COUNT(*)=2,'PASS','FAIL') result FROM products WHERE slug IN ('demo-floral-eau-de-parfum','demo-woody-eau-de-parfum');
SELECT 'demo_variants' check_name, COUNT(*) actual_value, IF(COUNT(*)=2,'PASS','FAIL') result FROM product_variants WHERE sku IN ('DEMO-FLORAL-050','DEMO-WOODY-050');
SELECT 'demo_inventory' check_name, COUNT(*) actual_value, IF(COUNT(*)=2,'PASS','FAIL') result FROM inventory i JOIN product_variants v ON v.id=i.variant_id WHERE v.sku IN ('DEMO-FLORAL-050','DEMO-WOODY-050');
SELECT 'demo_coupon' check_name, COUNT(*) actual_value, IF(COUNT(*)=1,'PASS','FAIL') result FROM coupons WHERE code='WELCOME-DEMO';
SELECT 'demo_newsletter' check_name, COUNT(*) actual_value, IF(COUNT(*)=1,'PASS','FAIL') result FROM newsletter_subscribers WHERE email='demo-subscriber@example.invalid';
SELECT 'orphan_variants' check_name, COUNT(*) actual_value, IF(COUNT(*)=0,'PASS','FAIL') result FROM product_variants pv LEFT JOIN products p ON p.id=pv.product_id WHERE p.id IS NULL;
