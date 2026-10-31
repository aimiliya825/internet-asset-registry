package com.asset.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端冒烟测试：H2(MySQL 兼容模式) + 真实 HTTP 调用，覆盖登记查重、状态流转、统计与看板。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiSmokeTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private com.asset.registry.repository.OrgUnitRepository unitRepository;

    @Autowired
    private com.asset.registry.repository.AssetTypeRepository typeRepository;

    @Autowired
    private com.asset.registry.repository.AssetTypeFieldRepository fieldRepository;

    @Autowired
    private com.asset.registry.repository.SysUserRepository userRepository;

    private static final String REGISTER_JSON = """
            {"assetName":"统一身份认证系统","typeCode":"app","assetIdent":"https://sso.demo.com",
             "extra":{"shortName":"SSO","level":"三级"},
             "unitId":1,"ownerName":"王琳","ownerPhone":"13700005566",
             "deployLocation":"自建机房 A-03","onlineDate":"2026-01-05"}
            """;

    @BeforeEach
    void seed() {
        if (unitRepository.count() > 0) {
            return;
        }
        var unit = new com.asset.registry.entity.OrgUnit();
        unit.setUnitCode("IT");
        unit.setUnitName("信息技术部");
        unit.setShortName("信息");
        unit.setSortNo(1);
        unit.setEnabled(true);
        unitRepository.save(unit);

        var type = new com.asset.registry.entity.AssetType();
        type.setTypeCode("app");
        type.setTypeName("应用系统");
        type.setColor("#7C3AED");
        type.setEvidenceHint("系统上线审批表");
        type.setSortNo(1);
        type.setEnabled(true);
        typeRepository.save(type);

        var field = new com.asset.registry.entity.AssetTypeField();
        field.setTypeCode("app");
        field.setFieldKey("level");
        field.setFieldLabel("等级保护级别");
        field.setFieldType("select");
        field.setRequired(true);
        field.setUniqueFlag(false);
        field.setOptions("一级,二级,三级,四级");
        field.setSortNo(1);
        fieldRepository.save(field);

        var user = new com.asset.registry.entity.SysUser();
        user.setUsername("wanglin");
        user.setRealName("王琳");
        user.setPhone("13700005566");
        user.setUnitId(1L);
        user.setRoleCode("ROLE_UNIT_ADMIN");
        user.setStatus(true);
        userRepository.save(user);
    }

    private HttpHeaders json() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-User-Id", "1");
        return h;
    }

    private JsonNode body(ResponseEntity<String> resp) throws Exception {
        return om.readTree(resp.getBody());
    }

    @Test
    void register_duplicate_blocked_and_stats_consistent() throws Exception {
        // 1) 首次登记成功，编号自动生成
        ResponseEntity<String> created = rest.exchange("/api/assets", HttpMethod.POST,
                new HttpEntity<>(REGISTER_JSON, json()), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = body(created).path("data");
        long id = data.path("asset").path("id").asLong();
        assertThat(data.path("asset").path("assetNo").asText()).isEqualTo("A1001");
        assertThat(data.path("asset").path("runStatus").asText()).isEqualTo("active");

        // 2) 重复标识被拦截（409）
        ResponseEntity<String> dup = rest.exchange("/api/assets", HttpMethod.POST,
                new HttpEntity<>(REGISTER_JSON, json()), String.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body(dup).path("message").asText()).contains("已登记");

        // 3) 台账查询命中
        ResponseEntity<String> list = rest.exchange("/api/assets?keyword=sso&page=1&size=10",
                HttpMethod.GET, new HttpEntity<>(json()), String.class);
        assertThat(body(list).path("data").path("total").asLong()).isEqualTo(1);

        // 4) 缺少变更原因时状态变更被拒绝
        ResponseEntity<String> bad = rest.exchange("/api/assets/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "stopped", "reason", "短"), json()), String.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 5) 合法状态变更 + 日志留痕
        ResponseEntity<String> stopped = rest.exchange("/api/assets/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "stopped", "reason", "设备老化待替换，先行停用"), json()), String.class);
        assertThat(stopped.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode logs = body(stopped).path("data").path("logs");
        assertThat(logs.isArray()).isTrue();
        assertThat(logs.get(0).path("changeType").asText()).isEqualTo("状态变更");

        // 6) 看板与统计口径一致
        ResponseEntity<String> dash = rest.exchange("/api/dashboard/summary", HttpMethod.GET,
                new HttpEntity<>(json()), String.class);
        assertThat(body(dash).path("data").path("total").asLong()).isEqualTo(1);
        assertThat(body(dash).path("data").path("dead").asLong()).isEqualTo(1);

        ResponseEntity<String> stats = rest.exchange("/api/stats/overview?dim=type", HttpMethod.GET,
                new HttpEntity<>(json()), String.class);
        assertThat(body(stats).path("data").path("series").get(0).path("value").asLong()).isEqualTo(1);

        // 7) 字典可用
        ResponseEntity<String> meta = rest.exchange("/api/meta", HttpMethod.GET,
                new HttpEntity<>(json()), String.class);
        assertThat(body(meta).path("data").path("types").get(0).path("code").asText()).isEqualTo("app");
    }
}
