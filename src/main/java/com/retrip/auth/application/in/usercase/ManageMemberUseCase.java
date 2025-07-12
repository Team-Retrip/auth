package com.retrip.auth.application.in.usercase;


import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.request.MemberUpdateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.response.MemberUpdateResponse;

public interface ManageMemberUseCase {
    MemberCreateResponse createUser(MemberCreateRequest request);

    MemberUpdateResponse updateUser(MemberUpdateRequest request);
}
