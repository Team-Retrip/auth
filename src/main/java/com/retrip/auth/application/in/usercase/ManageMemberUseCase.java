package com.retrip.auth.application.in.usercase;


import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;

public interface ManageMemberUseCase {
    MemberCreateResponse createUser(MemberCreateRequest request);
}
