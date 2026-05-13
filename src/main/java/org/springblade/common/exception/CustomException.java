package org.springblade.common.exception;

/**
 * 自定义异常
 * 专门用于业务逻辑中的自定义异常处理
 */
public class CustomException extends RuntimeException {
    
    private final Integer code;
    
    public CustomException(String message) {
        super(message);
        this.code = ErrorCodeEnum.BUSINESS_ERROR.getCode();
    }
    
    public CustomException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public CustomException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    
    public CustomException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
    
    public Integer getCode() {
        return code;
    }
} 