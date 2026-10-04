
def is_empty(s: str, trim: bool = True) -> bool:
    """
    判断字符串是否为空

    Args:
        s:    待判断的字符串
        trim: True 时去除首尾空白后再判断（等价于 Java isBlank）
              False 时仅判断 None / ""（等价于 Java isEmpty）
    Returns:
        bool: 为空返回 True，否则返回 False
    """
    if s is None:
        return True
    if not isinstance(s, str):
        raise TypeError(f"Expected str, got {type(s).__name__}")
    return len(s.strip()) == 0 if trim else len(s) == 0