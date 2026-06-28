package net.azisaba.mythicerrorfixer;

public class ParsedError {
    public enum ErrorType {
        MATERIAL,
        METASKILL,
        TYPE_MISSING,
        TEXTURE,
        OTHER
    }

    private final ErrorType type;
    private final String detail;
    private final String file;

    public ParsedError(ErrorType type, String detail, String file) {
        this.type = type;
        this.detail = detail;
        this.file = file;
    }

    public ErrorType getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }

    public String getFile() {
        return file;
    }
}
