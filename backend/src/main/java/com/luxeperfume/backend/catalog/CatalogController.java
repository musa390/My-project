package com.luxeperfume.backend.catalog;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class CatalogController {
    private final JdbcTemplate jdbc;

    @Value("${CLOUDINARY_CLOUD_NAME:}") private String cloudName;
    @Value("${CLOUDINARY_API_KEY:}") private String cloudApiKey;
    @Value("${CLOUDINARY_API_SECRET:}") private String cloudApiSecret;

    public CatalogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String text(Map<String,Object> body, String key, String fallback) {
        Object value = body.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
    }
    private static long number(Map<String,Object> body, String key, long fallback) {
        Object value = body.get(key);
        if (value == null) return fallback;
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " must be an integer"); }
    }
    private static boolean bool(Map<String,Object> body, String key, boolean fallback) {
        Object value = body.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
    private static String slug(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (result.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid slug is required");
        return result;
    }
    private Map<String,Object> one(String sql, Object... args) {
        List<Map<String,Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        return rows.getFirst();
    }
    private Map<String,Object> productView(long id) {
        Map<String,Object> product = one("""
            SELECT p.id,p.name,p.slug,p.short_description,p.description,p.product_type,p.status,
                   p.is_featured,p.seo_title,p.seo_description,p.created_at,p.updated_at,
                   c.id category_id,c.name category_name,c.slug category_slug,
                   b.id brand_id,b.name brand_name,b.slug brand_slug,
                   (SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) price_min,
                   (SELECT MAX(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) price_max,
                   COALESCE((SELECT SUM(i.quantity_on_hand-i.quantity_reserved)
                     FROM product_variants v JOIN inventory i ON i.variant_id=v.id
                     WHERE v.product_id=p.id AND v.is_active=TRUE),0) available_stock
            FROM products p JOIN categories c ON c.id=p.category_id
            LEFT JOIN brands b ON b.id=p.brand_id
            WHERE p.id=? AND p.deleted_at IS NULL
            """, id);
        product.put("images", jdbc.queryForList("""
            SELECT id,image_url,alt_text,sort_order,is_primary FROM product_images
            WHERE product_id=? ORDER BY is_primary DESC,sort_order,id
            """, id));
        product.put("variants", jdbc.queryForList("""
            SELECT v.id,v.sku,v.variant_name,v.option_values,v.volume_ml,v.price,v.compare_at_price,
                   v.weight_grams,v.is_default,v.is_active,
                   COALESCE(i.quantity_on_hand,0) quantity_on_hand,
                   COALESCE(i.quantity_reserved,0) quantity_reserved,
                   GREATEST(COALESCE(i.quantity_on_hand,0)-COALESCE(i.quantity_reserved,0),0) available_quantity
            FROM product_variants v LEFT JOIN inventory i ON i.variant_id=v.id
            WHERE v.product_id=? ORDER BY v.is_default DESC,v.id
            """, id));
        return product;
    }

    @GetMapping("/api/v1/catalog/products")
    public ApiResponse<Map<String,Object>> products(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) String category,
            @RequestParam(required=false) String brand,
            @RequestParam(required=false) String collection,
            @RequestParam(required=false) BigDecimal minPrice,
            @RequestParam(required=false) BigDecimal maxPrice,
            @RequestParam(required=false) Boolean inStock,
            @RequestParam(required=false) Boolean featured,
            @RequestParam(defaultValue="newest") String sort,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="12") int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        StringBuilder where = new StringBuilder(" WHERE p.status='ACTIVE' AND p.deleted_at IS NULL ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) { where.append(" AND (p.name LIKE ? OR p.short_description LIKE ? OR p.description LIKE ?)"); String term="%"+q.trim()+"%"; args.add(term);args.add(term);args.add(term); }
        if (category != null && !category.isBlank()) { where.append(" AND c.slug=?"); args.add(category); }
        if (brand != null && !brand.isBlank()) { where.append(" AND b.slug=?"); args.add(brand); }
        if (collection != null && !collection.isBlank()) { where.append(" AND EXISTS(SELECT 1 FROM collection_products cp JOIN collections co ON co.id=cp.collection_id WHERE cp.product_id=p.id AND co.slug=? AND co.is_active=TRUE)"); args.add(collection); }
        if (minPrice != null) { where.append(" AND EXISTS(SELECT 1 FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE AND v.price>=?)"); args.add(minPrice); }
        if (maxPrice != null) { where.append(" AND EXISTS(SELECT 1 FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE AND v.price<=?)"); args.add(maxPrice); }
        if (Boolean.TRUE.equals(inStock)) where.append(" AND EXISTS(SELECT 1 FROM product_variants v JOIN inventory i ON i.variant_id=v.id WHERE v.product_id=p.id AND v.is_active=TRUE AND i.quantity_on_hand>i.quantity_reserved)");
        if (Boolean.FALSE.equals(inStock)) where.append(" AND NOT EXISTS(SELECT 1 FROM product_variants v JOIN inventory i ON i.variant_id=v.id WHERE v.product_id=p.id AND v.is_active=TRUE AND i.quantity_on_hand>i.quantity_reserved)");
        if (featured != null) { where.append(" AND p.is_featured=?"); args.add(featured); }
        String order = switch (sort.toLowerCase(Locale.ROOT)) {
            case "oldest" -> "p.created_at ASC";
            case "name_asc" -> "p.name ASC";
            case "name_desc" -> "p.name DESC";
            case "price_asc" -> "(SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) ASC";
            case "price_desc" -> "(SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) DESC";
            case "newest" -> "p.created_at DESC";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort option");
        };
        String from = " FROM products p JOIN categories c ON c.id=p.category_id LEFT JOIN brands b ON b.id=p.brand_id ";
        Long total = jdbc.queryForObject("SELECT COUNT(*)"+from+where, Long.class, args.toArray());
        List<Map<String,Object>> items = jdbc.queryForList("""
            SELECT p.id,p.name,p.slug,p.short_description,p.product_type,p.is_featured,p.created_at,
                   c.name category_name,c.slug category_slug,b.name brand_name,
                   (SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) price_min,
                   (SELECT MAX(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) price_max,
                   EXISTS(SELECT 1 FROM product_variants v JOIN inventory i ON i.variant_id=v.id
                          WHERE v.product_id=p.id AND v.is_active=TRUE AND i.quantity_on_hand>i.quantity_reserved) available
            """ + from + where + " ORDER BY " + order + " LIMIT ? OFFSET ?",
                concat(args, size, page*size).toArray());
        return ApiResponse.success("Catalog products", Map.of("items",items,"page",page,"size",size,"totalElements",total==null?0:total,"totalPages",total==null?0:(int)Math.ceil(total/(double)size)));
    }
    private static List<Object> concat(List<Object> args,Object... tail) { List<Object> all=new ArrayList<>(args); all.addAll(Arrays.asList(tail)); return all; }

    @GetMapping("/api/v1/catalog/products/{slug}")
    public ApiResponse<Map<String,Object>> product(@PathVariable String slug) {
        Map<String,Object> row = one("SELECT id FROM products WHERE slug=? AND status='ACTIVE' AND deleted_at IS NULL", slug);
        Map<String,Object> view = productView(((Number)row.get("id")).longValue());
        if (!"ACTIVE".equals(String.valueOf(view.get("status")))) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
        return ApiResponse.success("Product detail",view);
    }

    @GetMapping("/api/v1/catalog/categories")
    public ApiResponse<List<Map<String,Object>>> categories() {
        return ApiResponse.success("Categories",jdbc.queryForList("SELECT id,parent_id,name,slug,description,image_url,sort_order FROM categories WHERE is_active=TRUE ORDER BY sort_order,name"));
    }
    @GetMapping("/api/v1/catalog/categories/{slug}")
    public ApiResponse<Map<String,Object>> category(@PathVariable String slug) {
        Map<String,Object> category=one("SELECT id,parent_id,name,slug,description,image_url,sort_order FROM categories WHERE slug=? AND is_active=TRUE",slug);
        category.put("products",jdbc.queryForList("SELECT p.id,p.name,p.slug,p.short_description FROM products p WHERE p.category_id=? AND p.status='ACTIVE' AND p.deleted_at IS NULL ORDER BY p.created_at DESC",category.get("id")));
        return ApiResponse.success("Category detail",category);
    }
    @GetMapping("/api/v1/catalog/brands")
    public ApiResponse<List<Map<String,Object>>> brands() {
        return ApiResponse.success("Brands",jdbc.queryForList("SELECT id,name,slug,description,logo_url FROM brands WHERE is_active=TRUE ORDER BY name"));
    }
    @GetMapping("/api/v1/catalog/collections")
    public ApiResponse<List<Map<String,Object>>> collections() {
        return ApiResponse.success("Collections",jdbc.queryForList("SELECT id,name,slug,description,cover_image_url,starts_at,ends_at FROM collections WHERE is_active=TRUE AND (starts_at IS NULL OR starts_at<=UTC_TIMESTAMP()) AND (ends_at IS NULL OR ends_at>=UTC_TIMESTAMP()) ORDER BY created_at DESC"));
    }
    @GetMapping("/api/v1/catalog/collections/{slug}")
    public ApiResponse<Map<String,Object>> collection(@PathVariable String slug) {
        Map<String,Object> c=one("SELECT id,name,slug,description,cover_image_url FROM collections WHERE slug=? AND is_active=TRUE AND (starts_at IS NULL OR starts_at<=UTC_TIMESTAMP()) AND (ends_at IS NULL OR ends_at>=UTC_TIMESTAMP())",slug);
        c.put("products",jdbc.queryForList("""
            SELECT p.id,p.name,p.slug,p.short_description,
             (SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) price_min
            FROM collection_products cp JOIN products p ON p.id=cp.product_id
            WHERE cp.collection_id=? AND p.status='ACTIVE' AND p.deleted_at IS NULL ORDER BY cp.sort_order,p.name
            """,c.get("id")));
        return ApiResponse.success("Collection detail",c);
    }
    @GetMapping("/api/v1/catalog/featured")
    public ApiResponse<List<Map<String,Object>>> featured() { return curated("p.is_featured=TRUE","p.created_at DESC"); }
    @GetMapping("/api/v1/catalog/new-arrivals")
    public ApiResponse<List<Map<String,Object>>> newArrivals() { return curated("p.created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 30 DAY)","p.created_at DESC"); }
    @GetMapping("/api/v1/catalog/best-sellers")
    public ApiResponse<List<Map<String,Object>>> bestSellers() {
        return ApiResponse.success("Best sellers",jdbc.queryForList("""
            SELECT p.id,p.name,p.slug,SUM(oi.quantity) units_sold,
              (SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) price_min
            FROM products p JOIN product_variants pv ON pv.product_id=p.id
            JOIN order_items oi ON oi.variant_id=pv.id JOIN customer_orders o ON o.id=oi.order_id
            WHERE p.status='ACTIVE' AND p.deleted_at IS NULL AND o.order_status NOT IN ('CANCELLED','RETURNED')
            GROUP BY p.id,p.name,p.slug ORDER BY units_sold DESC,p.name LIMIT 20
            """));
    }
    private ApiResponse<List<Map<String,Object>>> curated(String condition,String order) {
        String sql = "SELECT p.id,p.name,p.slug,p.short_description,p.is_featured " +
            "FROM products p WHERE p.status='ACTIVE' AND p.deleted_at IS NULL AND " +
            condition + " ORDER BY " + order + " LIMIT 20";
        return ApiResponse.success("Catalog products",jdbc.queryForList(sql));
    }

    @GetMapping("/api/v1/staff/catalog/products")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> staffProducts(@RequestParam(required=false) String q,
            @RequestParam(required=false) String status,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        if(page<0||size<1||size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        StringBuilder where=new StringBuilder(" WHERE p.deleted_at IS NULL ");
        List<Object> args=new ArrayList<>();
        if(q!=null&&!q.isBlank()){where.append(" AND (p.name LIKE ? OR p.slug LIKE ?)");String term="%"+q.trim()+"%";args.add(term);args.add(term);}
        if(status!=null&&!status.isBlank()){if(!Set.of("DRAFT","ACTIVE","ARCHIVED").contains(status.toUpperCase(Locale.ROOT)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid product status");where.append(" AND p.status=?");args.add(status.toUpperCase(Locale.ROOT));}
        String from=" FROM products p JOIN categories c ON c.id=p.category_id LEFT JOIN brands b ON b.id=p.brand_id ";
        Long total=jdbc.queryForObject("SELECT COUNT(*)"+from+where,Long.class,args.toArray());
        List<Map<String,Object>> items=jdbc.queryForList("SELECT p.id,p.name,p.slug,p.status,p.is_featured,p.created_at,c.name category_name,b.name brand_name"+from+where+" ORDER BY p.updated_at DESC LIMIT ? OFFSET ?",concat(args,size,page*size).toArray());
        return ApiResponse.success("Staff product catalogue",Map.of("items",items,"page",page,"size",size,"totalElements",total==null?0:total,"totalPages",total==null?0:(int)Math.ceil(total/(double)size)));
    }

    @PostMapping("/api/v1/staff/catalog/products")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> createProduct(@RequestBody Map<String,Object> body) {
        String name=text(body,"name",""); if(name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"name is required");
        String productSlug=slug(text(body,"slug",name));
        long categoryId=number(body,"categoryId",0);
        if(categoryId<1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"categoryId is required");
        String type=text(body,"productType","STANDARD").toUpperCase(Locale.ROOT);
        String status=text(body,"status","DRAFT").toUpperCase(Locale.ROOT);
        if(!Set.of("STANDARD","VARIABLE").contains(type)||!Set.of("DRAFT","ACTIVE","ARCHIVED").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid product type or status");
        Long brandId=body.get("brandId")==null?null:number(body,"brandId",0);
        jdbc.update("INSERT INTO products(category_id,brand_id,name,slug,short_description,description,product_type,status,is_featured,seo_title,seo_description) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            categoryId,brandId,name,productSlug,text(body,"shortDescription",null),text(body,"description",null),type,status,bool(body,"featured",false),text(body,"seoTitle",null),text(body,"seoDescription",null));
        Long id=jdbc.queryForObject("SELECT id FROM products WHERE slug=?",Long.class,productSlug);
        return ApiResponse.success("Product created",productView(id));
    }
    @PutMapping("/api/v1/staff/catalog/products/{id}")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> updateProduct(@PathVariable long id,@RequestBody Map<String,Object> body) {
        Map<String,Object> current=one("SELECT * FROM products WHERE id=? AND deleted_at IS NULL",id);
        String name=text(body,"name",String.valueOf(current.get("name")));
        String productSlug=slug(text(body,"slug",String.valueOf(current.get("slug"))));
        long categoryId=number(body,"categoryId",((Number)current.get("category_id")).longValue());
        Object brandId=body.containsKey("brandId")?body.get("brandId"):current.get("brand_id");
        String type=text(body,"productType",String.valueOf(current.get("product_type"))).toUpperCase(Locale.ROOT);
        String status=text(body,"status",String.valueOf(current.get("status"))).toUpperCase(Locale.ROOT);
        if(!Set.of("STANDARD","VARIABLE").contains(type)||!Set.of("DRAFT","ACTIVE","ARCHIVED").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid product type or status");
        jdbc.update("UPDATE products SET category_id=?,brand_id=?,name=?,slug=?,short_description=?,description=?,product_type=?,status=?,is_featured=?,seo_title=?,seo_description=? WHERE id=?",
            categoryId,brandId,name,productSlug,text(body,"shortDescription",(String)current.get("short_description")),text(body,"description",(String)current.get("description")),type,status,bool(body,"featured",Boolean.TRUE.equals(current.get("is_featured"))),text(body,"seoTitle",(String)current.get("seo_title")),text(body,"seoDescription",(String)current.get("seo_description")),id);
        return ApiResponse.success("Product updated",productView(id));
    }
    @DeleteMapping("/api/v1/staff/catalog/products/{id}")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> archiveProduct(@PathVariable long id) {
        int changed=jdbc.update("UPDATE products SET status='ARCHIVED',deleted_at=UTC_TIMESTAMP() WHERE id=? AND deleted_at IS NULL",id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
        return ApiResponse.success("Product archived",Map.of("id",id,"archived",true));
    }

    @PostMapping("/api/v1/staff/catalog/products/{id}/variants")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> createVariant(@PathVariable long id,@RequestBody Map<String,Object> body) {
        one("SELECT id FROM products WHERE id=? AND deleted_at IS NULL",id);
        String sku=text(body,"sku","").toUpperCase(Locale.ROOT), name=text(body,"variantName","");
        BigDecimal price=new BigDecimal(text(body,"price","-1"));
        if(sku.isBlank()||name.isBlank()||price.signum()<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"sku, variantName and non-negative price are required");
        Object options=body.get("optionValues");
        if(options!=null && !(options instanceof String)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"optionValues must be a valid JSON string");
        String optionsJson=(String)options;
        jdbc.update("INSERT INTO product_variants(product_id,sku,variant_name,option_values,volume_ml,price,compare_at_price,cost_price,weight_grams,is_default,is_active) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            id,sku,name,optionsJson,body.get("volumeMl"),price,body.get("compareAtPrice"),body.get("costPrice"),body.get("weightGrams"),bool(body,"isDefault",false),bool(body,"isActive",true));
        Long variantId=jdbc.queryForObject("SELECT id FROM product_variants WHERE sku=?",Long.class,sku);
        jdbc.update("INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level) VALUES(?,?,0,?)",variantId,number(body,"initialStock",0),number(body,"reorderLevel",0));
        return ApiResponse.success("Variant created",one("SELECT v.*,i.quantity_on_hand,i.quantity_reserved,i.reorder_level FROM product_variants v JOIN inventory i ON i.variant_id=v.id WHERE v.id=?",variantId));
    }
    @PutMapping("/api/v1/staff/catalog/variants/{variantId}")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> updateVariant(@PathVariable long variantId,@RequestBody Map<String,Object> body) {
        Map<String,Object> v=one("SELECT * FROM product_variants WHERE id=?",variantId);
        jdbc.update("UPDATE product_variants SET sku=?,variant_name=?,price=?,compare_at_price=?,is_default=?,is_active=? WHERE id=?",
            text(body,"sku",String.valueOf(v.get("sku"))),text(body,"variantName",String.valueOf(v.get("variant_name"))),
            new BigDecimal(text(body,"price",String.valueOf(v.get("price")))),body.getOrDefault("compareAtPrice",v.get("compare_at_price")),
            bool(body,"isDefault",Boolean.TRUE.equals(v.get("is_default"))),bool(body,"isActive",Boolean.TRUE.equals(v.get("is_active"))),variantId);
        return ApiResponse.success("Variant updated",one("SELECT * FROM product_variants WHERE id=?",variantId));
    }
    @DeleteMapping("/api/v1/staff/catalog/variants/{variantId}")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> deactivateVariant(@PathVariable long variantId) {
        int changed=jdbc.update("UPDATE product_variants SET is_active=FALSE,is_default=FALSE WHERE id=?",variantId);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Variant not found");
        return ApiResponse.success("Variant deactivated",Map.of("id",variantId,"active",false));
    }

    @PutMapping("/api/v1/staff/catalog/variants/{variantId}/inventory")
    @PreAuthorize("hasAuthority('PERM_inventory.write')")
    public ApiResponse<Map<String,Object>> updateInventory(@PathVariable long variantId,@RequestBody Map<String,Object> body) {
        one("SELECT id FROM product_variants WHERE id=?",variantId);
        long qty=number(body,"quantityOnHand",-1), reserved=number(body,"quantityReserved",0), reorder=number(body,"reorderLevel",0);
        if(qty<0||reserved<0||reserved>qty||reorder<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid inventory quantities");
        jdbc.update("INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE quantity_on_hand=VALUES(quantity_on_hand),quantity_reserved=VALUES(quantity_reserved),reorder_level=VALUES(reorder_level)",variantId,qty,reserved,reorder);
        return ApiResponse.success("Inventory updated",one("SELECT * FROM inventory WHERE variant_id=?",variantId));
    }

    @PostMapping("/api/v1/staff/catalog/products/{id}/images")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> addImage(@PathVariable long id,@RequestBody Map<String,Object> body) {
        one("SELECT id FROM products WHERE id=? AND deleted_at IS NULL",id);
        String url=text(body,"imageUrl","");
        if(!url.startsWith("https://")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"imageUrl must use HTTPS");
        boolean primary=bool(body,"primary",false);
        if(primary) jdbc.update("UPDATE product_images SET is_primary=FALSE WHERE product_id=?",id);
        jdbc.update("INSERT INTO product_images(product_id,image_url,alt_text,sort_order,is_primary) VALUES(?,?,?,?,?)",id,url,text(body,"altText",null),number(body,"sortOrder",0),primary);
        return ApiResponse.success("Product image added",one("SELECT * FROM product_images WHERE product_id=? AND image_url=? ORDER BY id DESC LIMIT 1",id,url));
    }
    @PutMapping("/api/v1/staff/catalog/images/{imageId}")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> updateImage(@PathVariable long imageId,@RequestBody Map<String,Object> body) {
        Map<String,Object> image=one("SELECT * FROM product_images WHERE id=?",imageId);
        boolean primary=bool(body,"primary",Boolean.TRUE.equals(image.get("is_primary")));
        if(primary) jdbc.update("UPDATE product_images SET is_primary=FALSE WHERE product_id=?",image.get("product_id"));
        String url=text(body,"imageUrl",String.valueOf(image.get("image_url")));
        if(!url.startsWith("https://")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"imageUrl must use HTTPS");
        jdbc.update("UPDATE product_images SET image_url=?,alt_text=?,sort_order=?,is_primary=? WHERE id=?",
            url,text(body,"altText",(String)image.get("alt_text")),number(body,"sortOrder",((Number)image.get("sort_order")).longValue()),primary,imageId);
        return ApiResponse.success("Product image updated",one("SELECT * FROM product_images WHERE id=?",imageId));
    }

    @DeleteMapping("/api/v1/staff/catalog/images/{imageId}")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> deleteImage(@PathVariable long imageId) {
        int changed=jdbc.update("DELETE FROM product_images WHERE id=?",imageId);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
        return ApiResponse.success("Product image deleted",Map.of("id",imageId));
    }

    @PostMapping("/api/v1/staff/catalog/categories")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> createCategory(@RequestBody Map<String,Object> body) {
        String name=text(body,"name",""); if(name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"name is required");
        jdbc.update("INSERT INTO categories(parent_id,name,slug,description,image_url,sort_order,is_active) VALUES(?,?,?,?,?,?,?)",
            body.get("parentId"),name,slug(text(body,"slug",name)),text(body,"description",null),text(body,"imageUrl",null),number(body,"sortOrder",0),bool(body,"active",true));
        return ApiResponse.success("Category created",one("SELECT * FROM categories WHERE slug=?",slug(text(body,"slug",name))));
    }
    @PutMapping("/api/v1/staff/catalog/categories/{id}")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> updateCategory(@PathVariable long id,@RequestBody Map<String,Object> body) {
        Map<String,Object> c=one("SELECT * FROM categories WHERE id=?",id);
        String name=text(body,"name",String.valueOf(c.get("name"))), s=slug(text(body,"slug",String.valueOf(c.get("slug"))));
        jdbc.update("UPDATE categories SET parent_id=?,name=?,slug=?,description=?,image_url=?,sort_order=?,is_active=? WHERE id=?",
            body.getOrDefault("parentId",c.get("parent_id")),name,s,text(body,"description",(String)c.get("description")),text(body,"imageUrl",(String)c.get("image_url")),number(body,"sortOrder",((Number)c.get("sort_order")).longValue()),bool(body,"active",Boolean.TRUE.equals(c.get("is_active"))),id);
        return ApiResponse.success("Category updated",one("SELECT * FROM categories WHERE id=?",id));
    }
    @DeleteMapping("/api/v1/staff/catalog/categories/{id}")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> archiveCategory(@PathVariable long id) {
        int n=jdbc.update("UPDATE categories SET is_active=FALSE WHERE id=?",id);
        if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found");
        return ApiResponse.success("Category deactivated",Map.of("id",id));
    }

    @PostMapping("/api/v1/staff/catalog/brands")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> createBrand(@RequestBody Map<String,Object> body) {
        String name=text(body,"name",""); if(name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"name is required");
        String s=slug(text(body,"slug",name));
        jdbc.update("INSERT INTO brands(name,slug,description,logo_url,is_active) VALUES(?,?,?,?,?)",name,s,text(body,"description",null),text(body,"logoUrl",null),bool(body,"active",true));
        return ApiResponse.success("Brand created",one("SELECT * FROM brands WHERE slug=?",s));
    }
    @PutMapping("/api/v1/staff/catalog/brands/{id}")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> updateBrand(@PathVariable long id,@RequestBody Map<String,Object> body) {
        Map<String,Object> b=one("SELECT * FROM brands WHERE id=?",id);
        String name=text(body,"name",String.valueOf(b.get("name"))),s=slug(text(body,"slug",String.valueOf(b.get("slug"))));
        jdbc.update("UPDATE brands SET name=?,slug=?,description=?,logo_url=?,is_active=? WHERE id=?",name,s,text(body,"description",(String)b.get("description")),text(body,"logoUrl",(String)b.get("logo_url")),bool(body,"active",Boolean.TRUE.equals(b.get("is_active"))),id);
        return ApiResponse.success("Brand updated",one("SELECT * FROM brands WHERE id=?",id));
    }
    @DeleteMapping("/api/v1/staff/catalog/brands/{id}")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> deactivateBrand(@PathVariable long id) {
        int n=jdbc.update("UPDATE brands SET is_active=FALSE WHERE id=?",id);
        if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Brand not found");
        return ApiResponse.success("Brand deactivated",Map.of("id",id));
    }

    @PostMapping("/api/v1/staff/catalog/collections")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> createCollection(@RequestBody Map<String,Object> body) {
        String name=text(body,"name","");if(name.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"name is required");
        String s=slug(text(body,"slug",name));
        jdbc.update("INSERT INTO collections(name,slug,description,cover_image_url,starts_at,ends_at,is_active) VALUES(?,?,?,?,?,?,?)",
            name,s,text(body,"description",null),text(body,"coverImageUrl",null),body.get("startsAt"),body.get("endsAt"),bool(body,"active",true));
        Map<String,Object> c=one("SELECT * FROM collections WHERE slug=?",s);
        if(body.get("productIds") instanceof List<?> ids) for(Object productId:ids) jdbc.update("INSERT IGNORE INTO collection_products(collection_id,product_id) VALUES(?,?)",c.get("id"),Long.parseLong(String.valueOf(productId)));
        return ApiResponse.success("Collection created",one("SELECT * FROM collections WHERE id=?",c.get("id")));
    }
    @PutMapping("/api/v1/staff/catalog/collections/{id}")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> updateCollection(@PathVariable long id,@RequestBody Map<String,Object> body) {
        Map<String,Object> c=one("SELECT * FROM collections WHERE id=?",id);
        String name=text(body,"name",String.valueOf(c.get("name"))),s=slug(text(body,"slug",String.valueOf(c.get("slug"))));
        jdbc.update("UPDATE collections SET name=?,slug=?,description=?,cover_image_url=?,starts_at=?,ends_at=?,is_active=? WHERE id=?",
            name,s,text(body,"description",(String)c.get("description")),text(body,"coverImageUrl",(String)c.get("cover_image_url")),body.getOrDefault("startsAt",c.get("starts_at")),body.getOrDefault("endsAt",c.get("ends_at")),bool(body,"active",Boolean.TRUE.equals(c.get("is_active"))),id);
        if(body.get("productIds") instanceof List<?> ids) {
            jdbc.update("DELETE FROM collection_products WHERE collection_id=?",id);
            for(Object productId:ids) jdbc.update("INSERT IGNORE INTO collection_products(collection_id,product_id) VALUES(?,?)",id,Long.parseLong(String.valueOf(productId)));
        }
        return ApiResponse.success("Collection updated",one("SELECT * FROM collections WHERE id=?",id));
    }
    @DeleteMapping("/api/v1/staff/catalog/collections/{id}")
    @PreAuthorize("hasAuthority('PERM_catalog.write')")
    public ApiResponse<Map<String,Object>> deactivateCollection(@PathVariable long id) {
        int n=jdbc.update("UPDATE collections SET is_active=FALSE WHERE id=?",id);
        if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Collection not found");
        return ApiResponse.success("Collection deactivated",Map.of("id",id));
    }

    @PostMapping("/api/v1/staff/catalog/cloudinary/signature")
    @PreAuthorize("hasAuthority('PERM_products.write')")
    public ApiResponse<Map<String,Object>> cloudinarySignature() {
        if(cloudName.isBlank()||cloudApiKey.isBlank()||cloudApiSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cloudinary is not configured");
        long timestamp=Instant.now().getEpochSecond();
        try {
            Mac mac=Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(cloudApiSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA1"));
            String signature=HexFormat.of().formatHex(mac.doFinal(("timestamp="+timestamp).getBytes(StandardCharsets.UTF_8)));
            return ApiResponse.success("Cloudinary upload signature",Map.of("cloudName",cloudName,"apiKey",cloudApiKey,"timestamp",timestamp,"signature",signature,"uploadUrl","https://api.cloudinary.com/v1_1/"+cloudName+"/image/upload"));
        } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not sign Cloudinary upload"); }
    }
}
