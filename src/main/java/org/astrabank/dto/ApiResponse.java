package org.astrabank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Nếu trường nào null thì không hiện trong JSON
public class ApiResponse<T> {
    private int code;      // Mã nội bộ (vd: 1000 là thành công, 1001 là lỗi...)
    private String message; // Thông báo (vd: "Tạo thành công")
    private T result;       // Dữ liệu chính (User, List<User>...)
}
