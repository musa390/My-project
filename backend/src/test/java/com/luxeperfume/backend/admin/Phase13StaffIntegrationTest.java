package com.luxeperfume.backend.admin;

import com.luxeperfume.backend.auth.StaffCustomerController;
import com.luxeperfume.backend.catalog.StaffBannerController;
import com.luxeperfume.backend.catalog.StaffCouponController;
import com.luxeperfume.backend.catalog.StaffReviewController;
import com.luxeperfume.backend.newsletter.StaffNewsletterController;
import com.luxeperfume.backend.payment.StaffPaymentQueryController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class Phase13StaffIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired StaffCustomerController customers;
    @Autowired StaffPaymentQueryController payments;
    @Autowired StaffCouponController coupons;
    @Autowired StaffBannerController banners;
    @Autowired StaffReviewController reviews;
    @Autowired StaffNewsletterController newsletter;
    @Autowired StaffAnalyticsController analytics;

    @AfterEach
    void clearSecurityContext(){SecurityContextHolder.clearContext();}

    @Test
    void staffManagementApisReadAndWriteRealDatabaseRecords(){
        String email="phase13-staff-"+UUID.randomUUID()+"@example.invalid";
        jdbc.update("""
                INSERT INTO users(email,password_hash,first_name,last_name,account_status,email_verified_at)
                VALUES(?, 'not-a-real-password-hash', 'Phase13', 'Staff', 'ACTIVE', UTC_TIMESTAMP())
                """,email);
        Long staffId=jdbc.queryForObject("SELECT id FROM users WHERE email=?",Long.class,email);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='ADMIN'",staffId);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(staffId,"unused",
                "ROLE_ADMIN","PERM_customers.read","PERM_payments.read","PERM_promotions.write",
                "PERM_reviews.moderate","PERM_newsletter.manage","PERM_dashboard.read","PERM_orders.write","PERM_staff.manage"));

        assertThat(customers.search(null,null,0,10).data()).containsKey("items");
        assertThat(payments.search(null,null,0,10).data()).containsKey("items");
        assertThat(reviews.list(null,null,0,10).data()).containsKey("items");
        assertThat(newsletter.list(null,null,0,10).data()).containsKey("items");
        assertThat(analytics.overview().data()).containsKeys("activeProducts","totalOrders","paidRevenue");

        String couponCode="P13"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();
        var coupon=coupons.create(new StaffCouponController.CouponRequest(couponCode,"Phase 13 integration test",
                "PERCENTAGE",new BigDecimal("10"),BigDecimal.ZERO,null,10,1,null,null,true));
        assertThat(coupon.data()).containsEntry("code",couponCode);
        assertThat(coupons.list(couponCode,null,0,10).data().get("items")).isNotEmpty();

        var banner=banners.create(new StaffBannerController.BannerRequest("Phase 13 test banner","Integration",
                "https://res.cloudinary.com/demo/image/upload/sample.jpg","/shop","HOME_HERO",null,null,99,false));
        assertThat(banner.data()).containsEntry("title","Phase 13 test banner");
        assertThat(banners.list("Phase 13 test banner",null,0,10).data().get("items")).isNotEmpty();
    }
}
