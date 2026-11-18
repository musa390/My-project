package com.luxeperfume.backend.integration.cloudinary;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/staff/catalog/images")
public class CloudinaryImageController {
    private final JdbcTemplate jdbc; private final RestClient client=RestClient.create();
    private final String cloudName,apiKey,apiSecret;
    public CloudinaryImageController(JdbcTemplate jdbc,@Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String apiKey,@Value("${CLOUDINARY_API_SECRET:}") String apiSecret) {
        this.jdbc=jdbc;this.cloudName=cloudName;this.apiKey=apiKey;this.apiSecret=apiSecret;
    }
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_products.write')")
    @Transactional
    public ApiResponse<Map<String,Object>> upload(@RequestParam long productId,@RequestParam(defaultValue="") @Size(max=255) String altText,
            @RequestParam(defaultValue="0") int sortOrder,@RequestParam(defaultValue="false") boolean primary,
            @RequestPart("file") MultipartFile file) {
        if(cloudName.isBlank()||apiKey.isBlank()||apiSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cloudinary is not configured");
        if(productId<=0||sortOrder<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image metadata");
        if(file==null||file.isEmpty()||file.getSize()>10L*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image must be between 1 byte and 10 MB");
        String type=file.getContentType();
        if(!("image/jpeg".equals(type)||"image/png".equals(type)||"image/webp".equals(type))) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Only JPEG, PNG and WebP images are allowed");
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE id=? AND deleted_at IS NULL",Integer.class,productId);
        if(exists==null||exists==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
        long timestamp=Instant.now().getEpochSecond(); String folder="luxe-perfume/products";
        String signature=sign("folder="+folder+"&timestamp="+timestamp);
        ByteArrayResource resource;
        try {
            byte[] bytes=file.getBytes();
            if (!matchesImageSignature(bytes, type))
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Image content does not match its declared type");
            resource=new ByteArrayResource(bytes){@Override public String getFilename(){return "product-image";}};
        } catch(ResponseStatusException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Could not read image file");
        }
        MultiValueMap<String,Object> form=new LinkedMultiValueMap<>();
        form.add("file",resource);form.add("api_key",apiKey);form.add("timestamp",String.valueOf(timestamp));form.add("folder",folder);form.add("signature",signature);
        Map<?,?> uploaded;
        try { uploaded=client.post().uri("https://api.cloudinary.com/v1_1/"+cloudName+"/image/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(Map.class); }
        catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary image upload failed");}
        if(uploaded==null||uploaded.get("secure_url")==null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary returned an invalid upload response");
        String url=String.valueOf(uploaded.get("secure_url"));
        if(!url.startsWith("https://res.cloudinary.com/"+cloudName+"/")) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary returned an unexpected image URL");
        if(primary) jdbc.update("UPDATE product_images SET is_primary=FALSE WHERE product_id=?",productId);
        jdbc.update("INSERT INTO product_images(product_id,image_url,alt_text,sort_order,is_primary) VALUES(?,?,?,?,?)",productId,url,altText.isBlank()?null:altText.trim(),sortOrder,primary);
        Long imageId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        return ApiResponse.success("Product image uploaded",Map.of("id",imageId,"imageUrl",url,"isPrimary",primary));
    }
    private static boolean matchesImageSignature(byte[] bytes,String type) {
        if ("image/jpeg".equals(type))
            return bytes.length>=3 && (bytes[0]&255)==0xFF && (bytes[1]&255)==0xD8 && (bytes[2]&255)==0xFF;
        if ("image/png".equals(type))
            return bytes.length>=8 && (bytes[0]&255)==0x89 && bytes[1]==0x50 && bytes[2]==0x4E
                    && bytes[3]==0x47 && bytes[4]==0x0D && bytes[5]==0x0A && bytes[6]==0x1A && bytes[7]==0x0A;
        if ("image/webp".equals(type))
            return bytes.length>=12 && bytes[0]=='R' && bytes[1]=='I' && bytes[2]=='F' && bytes[3]=='F'
                    && bytes[8]=='W' && bytes[9]=='E' && bytes[10]=='B' && bytes[11]=='P';
        return false;
    }

    private String sign(String value){try{Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA1"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not sign Cloudinary upload");}}
}
