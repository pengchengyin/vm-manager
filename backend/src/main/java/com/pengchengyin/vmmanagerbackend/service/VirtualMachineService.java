package com.pengchengyin.vmmanagerbackend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.pengchengyin.vmmanagerbackend.model.CreateVmRequest;
import com.pengchengyin.vmmanagerbackend.model.VmInfo;
import com.pengchengyin.vmmanagerbackend.model.VmStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * 虚拟机管理服务
 */
@Slf4j
@Service
public class VirtualMachineService {

    @Autowired
    private LibvirtConnectionService connectionService;

    /**
     * 获取所有虚拟机列表
     */
    public List<VmInfo> listAllVms() throws LibvirtException {
        Connect connect = connectionService.getConnection();
        List<VmInfo> vmList = new ArrayList<>();

        // 获取所有域（虚拟机）
        int[] domainIds = connect.listDomains();
        for (int id : domainIds) {
            Domain domain = connect.domainLookupByID(id);
            vmList.add(convertToVmInfo(domain));
        }

        // 获取所有已定义的域（未运行的）
        String[] definedDomains = connect.listDefinedDomains();
        for (String name : definedDomains) {
            Domain domain = connect.domainLookupByName(name);
            vmList.add(convertToVmInfo(domain));
        }

        return vmList;
    }

    /**
     * 根据名称获取虚拟机信息
     */
    public VmInfo getVmByName(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        return convertToVmInfo(domain);
    }

    /**
     * 根据UUID获取虚拟机信息
     */
    public VmInfo getVmByUuid(String uuid) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByUUIDString(uuid);
        return convertToVmInfo(domain);
    }

    /**
     * 创建虚拟机
     */
    public VmInfo createVm(CreateVmRequest request) throws LibvirtException {
        Connect connect = connectionService.getConnection();

        // 如果提供了XML配置文件路径，使用XML配置
        if (request.getXmlConfigPath() != null && !request.getXmlConfigPath().isEmpty()) {
            return createVmFromXml(connect, request);
        } else {
            // 使用参数创建
            return createVmFromParams(connect, request);
        }
    }

    /**
     * 从XML配置文件创建虚拟机
     */
    private VmInfo createVmFromXml(Connect connect, CreateVmRequest request) throws LibvirtException {
        String xmlContent = generateVmXml(request);
        Domain domain = connect.domainDefineXML(xmlContent);
        log.info("虚拟机已定义: {}", request.getName());
        return convertToVmInfo(domain);
    }

    /**
     * 从参数创建虚拟机
     */
    private VmInfo createVmFromParams(Connect connect, CreateVmRequest request) throws LibvirtException {
        String xmlContent = generateVmXml(request);
        Domain domain = connect.domainDefineXML(xmlContent);
        log.info("虚拟机已创建: {}", request.getName());
        return convertToVmInfo(domain);
    }

    /**
     * 生成虚拟机XML配置
     */
    private String generateVmXml(CreateVmRequest request) {
        try {
            // 生成UUID
            String uuid = java.util.UUID.randomUUID().toString();
            
            // 从模板文件读取XML内容
            ClassPathResource resource = new ClassPathResource("templates/vm-template.xml");
            String templateContent = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()), StandardCharsets.UTF_8);
            
            // 使用参数替换模板中的占位符
            // 注意：模板中的第一个空字符串占位符是为了保持兼容性
            return String.format(templateContent, 
                request.getName(), uuid,
                request.getMemoryMB() * 1024, request.getMemoryMB() * 1024,
                request.getCpuCount(), "", request.getDiskImagePath(), request.getNetworkName());
        } catch (IOException e) {
            log.error("读取虚拟机XML模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法读取虚拟机XML模板", e);
        }
    }

    /**
     * 销毁虚拟机（删除）
     */
    public void destroyVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);

        // 如果正在运行，先关闭
        if (domain.isActive() == 1) {
            domain.destroy();
            log.info("虚拟机 {} 已强制关闭", name);
        }

        // 取消定义（删除）
        domain.undefine();
        log.info("虚拟机 {} 已删除", name);
    }

    /**
     * 启动虚拟机
     */
    public void startVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.create();
        log.info("虚拟机 {} 已启动", name);
    }

    /**
     * 关闭虚拟机（优雅关闭）
     */
    public void shutdownVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.shutdown();
        log.info("虚拟机 {} 已发送关闭信号", name);
    }

    /**
     * 强制关闭虚拟机
     */
    public void forceShutdownVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.destroy();
        log.info("虚拟机 {} 已强制关闭", name);
    }

    /**
     * 重启虚拟机
     */
    public void rebootVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.reboot(0);
        log.info("虚拟机 {} 已重启", name);
    }

    /**
     * 暂停虚拟机
     */
    public void suspendVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.suspend();
        log.info("虚拟机 {} 已暂停", name);
    }

    /**
     * 恢复虚拟机
     */
    public void resumeVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        domain.resume();
        log.info("虚拟机 {} 已恢复", name);
    }

    /**
     * 获取虚拟机状态
     */
    public VmStatus getVmStatus(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        DomainInfo info = domain.getInfo();
        return convertDomainStateToVmStatus(info.state);
    }

    /**
     * 监控虚拟机状态（实时信息）
     */
    public VmInfo monitorVm(String name) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        return convertToVmInfo(domain);
    }

    /**
     * 将Domain对象转换为VmInfo
     */
    private VmInfo convertToVmInfo(Domain domain) throws LibvirtException {
        DomainInfo info = domain.getInfo();
        VmStatus status = convertDomainStateToVmStatus(info.state);

        // 获取XML配置（一次性获取，供后续使用）
        String xmlDesc = null;
        try {
            xmlDesc = domain.getXMLDesc(0);
        } catch (Exception e) {
            log.debug("获取XML配置失败: {}", e.getMessage());
        }

        // 获取网络接口
        List<String> interfaces = new ArrayList<>();
        if (xmlDesc != null) {
            try {
                // 使用正则表达式提取接口的target dev属性（如vnet0, eth0等）
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "<interface[^>]*>.*?<target\\s+dev=['\"]([^'\"]+)['\"]", 
                        java.util.regex.Pattern.DOTALL
                );
                java.util.regex.Matcher matcher = pattern.matcher(xmlDesc);
                while (matcher.find()) {
                    String iface = matcher.group(1);
                    if (iface != null && !iface.isEmpty()) {
                        interfaces.add(iface);
                    }
                }
                // 如果没有找到target dev，尝试查找mac地址
                if (interfaces.isEmpty()) {
                    pattern = java.util.regex.Pattern.compile(
                            "<interface[^>]*>.*?<mac\\s+address=['\"]([^'\"]+)['\"]", 
                            java.util.regex.Pattern.DOTALL
                    );
                    matcher = pattern.matcher(xmlDesc);
                    while (matcher.find()) {
                        String mac = matcher.group(1);
                        if (mac != null && !mac.isEmpty()) {
                            interfaces.add(mac);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("解析网络接口失败: {}", e.getMessage());
            }
        }

        // 获取磁盘
        List<String> disks = new ArrayList<>();
        if (xmlDesc != null) {
            try {
                // 使用正则表达式提取磁盘路径
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "<disk[^>]*>.*?<source\\s+file=['\"]([^'\"]+)['\"]", 
                        java.util.regex.Pattern.DOTALL
                );
                java.util.regex.Matcher matcher = pattern.matcher(xmlDesc);
                while (matcher.find()) {
                    String diskPath = matcher.group(1);
                    if (diskPath != null && !diskPath.isEmpty()) {
                        disks.add(diskPath);
                    }
                }
            } catch (Exception e) {
                log.debug("解析磁盘信息失败: {}", e.getMessage());
            }
        }

        // 解析VNC信息
        String vncHost = null;
        Integer vncPort = null;
        if (xmlDesc != null) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "<graphics[^>]*type=['\"]vnc['\"][^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m = pattern.matcher(xmlDesc);
                if (m.find()) {
                    String tag = m.group(0);
                    java.util.regex.Pattern portP = java.util.regex.Pattern.compile("port=['\"](-?\\d+)['\"]");
                    java.util.regex.Pattern hostP = java.util.regex.Pattern.compile("listen=['\"]([^'\"]*)['\"]");
                    java.util.regex.Matcher pm = portP.matcher(tag);
                    java.util.regex.Matcher hm = hostP.matcher(tag);
                    if (pm.find()) {
                        try { vncPort = Integer.parseInt(pm.group(1)); } catch (Exception ignore) {}
                    }
                    if (hm.find()) { vncHost = hm.group(1); }
                }
            } catch (Exception e) {
                log.debug("解析VNC信息失败: {}", e.getMessage());
            }
        }

        // 计算CPU使用率（需要获取两次CPU时间差）
        double cpuUsage = 0.0;
        try {
            if (info.state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                long cpuTime = info.cpuTime;
                // 这里简化处理，实际需要计算时间差
                cpuUsage = info.nrVirtCpu > 0 ? (cpuTime / 1000000.0) : 0.0;
            }
        } catch (Exception e) {
            log.debug("计算CPU使用率失败: {}", e.getMessage());
        }

        return VmInfo.builder()
                .name(domain.getName())
                .uuid(domain.getUUIDString())
                .status(status)
                .statusDescription(status.getDescription())
                .maxMemory(info.maxMem * 1024) // 转换为字节
                .currentMemory(info.memory * 1024) // 转换为字节
                .cpuCount(info.nrVirtCpu)
                .cpuUsage(cpuUsage)
                .runTime(info.nrVirtCpu > 0 ? info.cpuTime / 1000000000L : 0) // 转换为秒
                .persistent(domain.isPersistent() == 1)
                .networkInterfaces(interfaces)
                .disks(disks)
                .vncHost(vncHost)
                .vncPort(vncPort)
                .build();
    }

    /**
     * 通过QEMU Guest Agent修改来宾系统用户密码
     * 需要来宾系统安装并运行 qemu-guest-agent
     */
    public void changeGuestPassword(String name, String username, String password, boolean encrypted) throws LibvirtException {
        Connect connect = connectionService.getConnection();
        Domain domain = connect.domainLookupByName(name);
        // QGA 命令：guest-set-user-password
        String payload = String.format("{\"execute\":\"guest-set-user-password\",\"arguments\":{\"username\":\"%s\",\"password\":\"%s\",\"encrypted\":%s}}",
                username.replace("\\", "\\\\").replace("\"", "\\\""),
                password.replace("\\", "\\\\").replace("\"", "\\\""),
                encrypted ? "true" : "false");
        // timeout: 10s, flags: 0
        domain.qemuAgentCommand(payload, 10, 0);
    }

    /**
     * 将DomainState枚举转换为VmStatus
     */
    private VmStatus convertDomainStateToVmStatus(DomainInfo.DomainState domainState) {
        if (domainState == null) {
            return VmStatus.NOSTATE;
        }
        
        switch (domainState) {
            case VIR_DOMAIN_NOSTATE:
                return VmStatus.NOSTATE;
            case VIR_DOMAIN_RUNNING:
                return VmStatus.RUNNING;
            case VIR_DOMAIN_BLOCKED:
                return VmStatus.BLOCKED;
            case VIR_DOMAIN_PAUSED:
                return VmStatus.PAUSED;
            case VIR_DOMAIN_SHUTDOWN:
                return VmStatus.SHUTDOWN;
            case VIR_DOMAIN_SHUTOFF:
                return VmStatus.SHUTOFF;
            case VIR_DOMAIN_CRASHED:
                return VmStatus.CRASHED;
            default:
                return VmStatus.NOSTATE;
        }
    }
}

