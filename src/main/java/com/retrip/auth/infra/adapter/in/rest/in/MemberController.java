package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 관련 API")
public class MemberController {

    private final ManageMemberUseCase manageMemberUseCase;

    @PostMapping
    public MemberCreateResponse createUser(
        @RequestBody MemberCreateRequest request
    ){
        return manageMemberUseCase.createUser(request);
    }
}
