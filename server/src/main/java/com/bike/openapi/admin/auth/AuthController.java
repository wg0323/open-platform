package com.bike.openapi.admin.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.auth.dto.CaptchaVo;
import com.bike.openapi.admin.auth.dto.ChangePasswordRequest;
import com.bike.openapi.admin.auth.dto.LoginRequest;
import com.bike.openapi.admin.auth.dto.LoginVo;
import com.bike.openapi.admin.auth.dto.MeVo;
import com.bike.openapi.admin.auth.dto.UserBriefVo;
import com.bike.openapi.admin.menu.SysMenuService;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.domain.SysUser;
import com.bike.openapi.mapper.SysUserMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * 管理台认证：验证码、登录、登出、当前用户、改密。
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/auth")
public class AuthController {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN = 7;

    private final CaptchaService captchaService;
    private final LoginUserService loginUserService;
    private final SysMenuService sysMenuService;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    /**
     * 生成图形验证码。
     *
     * @return 验证码标识与 base64 图片
     */
    @GetMapping("/captcha")
    public ApiResult<CaptchaVo> captcha() {
        return ApiResult.ok(captchaService.generate());
    }

    /**
     * 登录：先校验验证码，再校验账号密码。
     *
     * @param request 登录请求
     * @return token 与用户信息
     */
    @PostMapping("/login")
    public ApiResult<LoginVo> login(@Valid @RequestBody LoginRequest request) {
        captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());
        LoginUser loginUser = loginUserService.loadByUsername(request.getUsername());
        if (loginUser == null || !passwordEncoder.matches(request.getPassword(), loginUser.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码不正确");
        }
        if (!loginUser.isEnabled()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号已停用");
        }
        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, loginUser.getUserId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now()));
        String token = jwtService.issue(loginUser.getUserId(), loginUser.getUsername(), loginUser.getTokenVersion());
        return ApiResult.ok(LoginVo.of(token, loginUser));
    }

    /**
     * 登出：把当前 token 的 jti 写入黑名单。
     *
     * @param request 请求
     * @return 空数据
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                blacklistService.blacklist(jwtService.jti(claims), jwtService.remainingSeconds(claims));
            } catch (JwtException | IllegalArgumentException ignored) {
                // token 已失效，无需再入黑名单
            }
        }
        return ApiResult.ok(null);
    }

    /**
     * 当前用户信息、权限码与菜单树。
     *
     * @param loginUser 登录主体
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ApiResult<MeVo> me(@AuthenticationPrincipal LoginUser loginUser) {
        MeVo vo = new MeVo();
        vo.setUser(UserBriefVo.of(loginUser));
        vo.setPerms(loginUser.getPerms());
        vo.setMenus(sysMenuService.treeOf(loginUser));
        vo.setMustChangePassword(loginUser.isMustChangePassword());
        return ApiResult.ok(vo);
    }

    /**
     * 修改本人密码，成功后 token_version 递增，旧 token 失效。
     *
     * @param loginUser 登录主体
     * @param request   改密请求
     * @return 空数据
     */
    @PutMapping("/password")
    public ApiResult<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), loginUser.getPassword())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "原密码不正确");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(request.getNewPassword(), loginUser.getPassword())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(loginUser.getUserId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        update.setMustChangePassword(Boolean.FALSE);
        update.setTokenVersion(loginUser.getTokenVersion() + 1);
        userMapper.updateById(update);
        return ApiResult.ok(null);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        return header.substring(TOKEN_BEGIN).trim();
    }
}
