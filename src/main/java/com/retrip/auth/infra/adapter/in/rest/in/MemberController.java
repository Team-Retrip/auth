package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.request.MemberDeleteRequest;
import com.retrip.auth.application.in.request.MemberUpdateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.response.MemberUpdateResponse;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 관련 API")
public class MemberController {

    private final ManageMemberUseCase manageMemberUseCase;

    @PostMapping
    @Schema(description = "회원 가입")
    public ApiResponse<MemberCreateResponse> createUser(
        @RequestBody MemberCreateRequest request
    ){
        return ApiResponse.created(manageMemberUseCase.createUser(request));
    }

    @PutMapping
    @Schema(description = "회원 정보 수정")
    public ApiResponse<MemberUpdateResponse> updateUser(
            @RequestBody MemberUpdateRequest request
    ){
        return ApiResponse.ok(manageMemberUseCase.updateUser(request));
    }

    @DeleteMapping()
    @Schema(description = "회원 정보 삭제")
    public ApiResponse<?> deleteUser(
            @RequestBody MemberDeleteRequest request
    ){
        manageMemberUseCase.deleteUser(request);
        return ApiResponse.noContent();
    }
}
