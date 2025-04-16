package xiaozhi.modules.device.controller;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import xiaozhi.modules.device.dto.DeviceReportReqDTO;
import xiaozhi.modules.device.dto.DeviceReportRespDTO;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.device.utils.NetworkUtil;
import xiaozhi.modules.sys.service.SysParamsService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;

@Tag(name = "设备管理", description = "OTA 相关接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ota/")
public class OTAController {
    private final DeviceService deviceService;
    private final SysParamsService SysParamsService;

    @Operation(summary = "检查 OTA 版本和设备激活状态")
    @PostMapping
    public ResponseEntity<String> checkOTAVersion(
            @RequestBody DeviceReportReqDTO deviceReportReqDTO,
            @Parameter(name = "Device-Id", description = "设备唯一标识", required = true, in = ParameterIn.HEADER) @RequestHeader("Device-Id") String deviceId,
            @Parameter(name = "Client-Id", description = "客户端标识", required = false, in = ParameterIn.HEADER) @RequestHeader(value = "Client-Id", required = false) String clientId) {
        if (StringUtils.isBlank(deviceId)) {
            return createResponse(DeviceReportRespDTO.createError("Device ID is required"));
        }
        if (StringUtils.isBlank(clientId)) {
            clientId = deviceId;
        }
        String macAddress = deviceReportReqDTO.getMacAddress();
        boolean macAddressValid = NetworkUtil.isMacAddressValid(macAddress);
        // 设备Id和Mac地址应是一致的, 并且必须需要application字段
        if (!deviceId.equals(macAddress) || !macAddressValid || deviceReportReqDTO.getApplication() == null) {
            return createResponse(DeviceReportRespDTO.createError("Invalid OTA request"));
        }
        return createResponse(deviceService.checkDeviceActive(macAddress, clientId, deviceReportReqDTO));
    }

    @GetMapping
    public ResponseEntity<String> getOTAPrompt() {
        return createResponse(DeviceReportRespDTO.createError("请提交正确的ota参数"));
    }

    @SneakyThrows
    private ResponseEntity<String> createResponse(DeviceReportRespDTO deviceReportRespDTO) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String json = objectMapper.writeValueAsString(deviceReportRespDTO);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(jsonBytes.length)
                .body(json);
    }

    // 新增下载接口
    @Operation(summary = "下载固件文件")
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFirmware() throws IOException {
        // 从数据库获取参数        
        String otaUrl = SysParamsService.getValue("ota.url", false);
        String otaFileName = SysParamsService.getValue("ota.filename", false);
        log.info("OTA文件下载参数 - URL: {}, 文件名: {}", otaUrl, otaFileName);
        
        if (StringUtils.isAnyBlank(otaUrl, otaFileName)) {
            log.warn("缺少必要参数 ota.url 或 ota.filename");
            return ResponseEntity.status(400).body(null);
        }

        // 构造文件路径
        URI uri = URI.create(otaUrl);
        Path filePath = Paths.get("./",uri.getPath(), otaFileName).toAbsolutePath();        
        log.debug("生成固件文件路径: {}", filePath);
        
        Resource resource = new UrlResource(filePath.toUri());
        log.info("尝试加载固件资源: {}", resource.getFilename());

        if (!resource.exists() || !resource.isReadable()) {
            log.error("固件文件不存在或不可读: {}", filePath);
            return ResponseEntity.status(404).body(null);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + otaFileName + "\"")
                .body(resource);
    }
}
