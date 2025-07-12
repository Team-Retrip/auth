package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.request.MemberUpdateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.response.MemberUpdateResponse;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
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
    public MemberCreateResponse createUser(
        @RequestBody MemberCreateRequest request
    ){
        return manageMemberUseCase.createUser(request);
    }

    @PutMapping
    @Schema(description = "회원 정보 수정")
    public MemberUpdateResponse updateUser(
            @RequestBody MemberUpdateRequest request
    ){
        return manageMemberUseCase.updateUser(request);
    }
}
