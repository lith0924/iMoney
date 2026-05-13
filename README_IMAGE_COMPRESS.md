# 图片压缩功能说明

## 概述
图片压缩功能支持压缩图片文件大小，并新增了格式选择和尺寸缩放功能。同时支持将多张图片打包成ZIP文件。

## API接口

### 1. 单张图片压缩
**接口地址：** `POST /images/compress-image`

**请求参数：**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| imageUrl | String | 是 | - | 图片URL地址 |
| quality | Float | 否 | 0.8 | 压缩质量(0.1-1.0)，1.0为最高质量 |
| format | String | 否 | "jpg" | 压缩格式，支持"jpg"或"png" |
| scale | Integer | 否 | 100 | 尺寸缩放比例(1-100)，100为原尺寸 |

**请求示例：**
```bash
curl -X POST "http://localhost:8080/images/compress-image" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "imageUrl=https://example.com/image.jpg&quality=0.8&format=jpg&scale=80"
```

**响应示例：**
```json
{
  "code": 200,
  "success": true,
  "data": "https://oss.example.com/compressed_image.jpg",
  "msg": "操作成功"
}
```

### 2. 批量图片ZIP压缩
**接口地址：** `POST /images/compress-zip`

**请求参数：**
```json
{
  "imageUrls": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.png",
    "https://example.com/image3.jpg"
  ],
  "zipFileName": "my_photos"
}
```

**参数说明：**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| imageUrls | List<String> | 是 | - | 图片URL数组，最多100张 |
| zipFileName | String | 否 | "images" | ZIP文件名称（不含扩展名） |

**响应示例：**
```json
{
  "code": 200,
  "success": true,
  "data": "https://oss.example.com/my_photos_1703123456789_Ab3x9K2m.zip",
  "msg": "操作成功"
}
```

### 3. 图片KB放大
**接口地址：** `POST /images/enlarge-image-kb`

**请求参数：**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| imageUrl | String | 是 | - | 图片URL地址 |
| targetKB | Integer | 是 | - | 目标KB数(1-10000) |

**说明：** 输出格式会根据上传图片的后缀自动确定，支持jpg、png、gif、bmp、webp等格式

**请求示例：**
```bash
curl -X POST "http://localhost:8080/images/enlarge-image-kb" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "imageUrl=https://example.com/image.jpg&targetKB=2048"
```

**响应示例：**
```json
{
  "code": 200,
  "success": true,
  "data": "https://oss.example.com/enlarged_image.jpg",
  "msg": "操作成功"
}
```

## 功能特性

### 1. 单张图片压缩
- **压缩质量控制**：支持0.1到1.0之间的压缩质量设置
- **格式选择**：支持输出为JPG格式（有损压缩，文件较小）或PNG格式（无损压缩，文件较大）
- **尺寸缩放**：支持1%到100%的缩放比例，使用高质量的双线性插值算法
- **智能处理**：自动验证输入参数，优化内存使用，及时释放资源

### 2. 批量图片ZIP压缩
- **自定义文件名**：支持指定ZIP文件名称，系统自动添加时间戳确保唯一性
- **智能路径管理**：在临时路径中创建带随机数的子目录，防止文件冲突
- **自动扩展名**：自动保留原始图片的文件扩展名
- **批量处理**：支持最多100张图片同时处理
- **错误容错**：单张图片下载失败不影响其他图片的处理
- **真正自定义文件名**：绕过系统默认的文件名重命名机制，保持用户指定的文件名

### 3. 图片KB放大
- **智能KB计算**：根据目标KB数自动计算需要的图片尺寸
- **高质量放大**：使用双三次插值算法，保证放大后的图片质量
- **二次放大优化**：首次放大未达到目标时，自动进行二次放大
- **自动格式检测**：根据上传图片后缀自动确定输出格式，支持多种图片格式
- **智能判断**：如果当前图片已满足KB要求，直接返回原图

### 4. 通用特性
- **参数验证**：完善的输入参数验证和错误处理
- **日志记录**：详细的操作日志，便于调试和监控
- **资源管理**：自动清理临时文件和目录
- **OSS集成**：自动上传到OSS存储

## 使用场景

### 1. 单张图片优化
```bash
# 网页优化：80%质量，60%尺寸，JPG格式
POST /images/compress-image?imageUrl=...&quality=0.8&format=jpg&scale=60

# 移动端适配：70%质量，50%尺寸，JPG格式
POST /images/compress-image?imageUrl=...&quality=0.7&format=jpg&scale=50

# 高质量保存：90%质量，原尺寸，PNG格式
POST /images/compress-image?imageUrl=...&quality=0.9&format=png&scale=100
```

### 2. 批量图片打包
```bash
# 用户相册打包
POST /images/compress-zip
{
  "imageUrls": ["url1", "url2", "url3"],
  "zipFileName": "vacation_photos"
}

# 产品图片打包
POST /images/compress-zip
{
  "imageUrls": ["url1", "url2"],
  "zipFileName": "product_images"
}
```

### 3. 图片KB放大
```bash
# 将图片放大到2MB（格式自动检测）
POST /images/enlarge-image-kb?imageUrl=...&targetKB=2048

# 将图片放大到1MB（格式自动检测）
POST /images/enlarge-image-kb?imageUrl=...&targetKB=1024

# 将小图片放大到500KB（格式自动检测）
POST /images/enlarge-image-kb?imageUrl=...&targetKB=500
```

**格式自动检测说明：**
- `.jpg` 或 `.jpeg` → 输出JPG格式
- `.png` → 输出PNG格式  
- `.gif` → 输出GIF格式
- `.bmp` → 输出BMP格式
- `.webp` → 输出WebP格式
- 其他格式 → 默认输出JPG格式

## 自定义文件名功能详解

### ZIP压缩自定义文件名

**问题背景：**
原有的 `uploadOss` 方法会使用 `IdUtils.nextId()` 自动重命名文件，导致用户指定的文件名失效。

**解决方案：**
1. **直接OSS上传**：绕过 `uploadOss` 方法，直接使用阿里云OSS SDK上传
2. **保持文件名**：完全保持用户指定的文件名，不进行任何重命名
3. **回退机制**：如果直接上传失败，自动回退到原有方法

**技术实现：**
```java
// 在 SysFileServiceImpl 中实现，直接使用OSS SDK上传
public String uploadZipToOssWithCustomName(byte[] fileContent, String customFileName) {
    // 构建OSS文件路径
    String ossKey = "zip/" + customFileName + ".zip";
    
    // 使用阿里云OSS SDK直接上传
    com.aliyun.oss.OSS ossClient = new com.aliyun.oss.OSSClientBuilder()
        .build(region, OSSAccessKeyId, accessKeySecret);
    
    // 使用用户指定的文件名作为OSS Key
    ossClient.putObject(bucket, ossKey, inputStream, metadata);
}

// 在 ImageFileController 中调用
byte[] zipContent = java.nio.file.Files.readAllBytes(tempZipFile.toPath());
String ossUrl = sysFileService.uploadZipToOssWithCustomName(zipContent, zipFileName);
```

**测试用例：**
```bash
# 测试自定义文件名功能
POST /images/compress-zip
{
  "imageUrls": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.png"
  ],
  "zipFileName": "my_custom_photos"
}

# 预期结果：
# - 生成的ZIP文件名为：my_custom_photos_1703123456789.zip
# - OSS路径为：zip/my_custom_photos_1703123456789.zip
# - 完全保持用户指定的文件名前缀
```

## 架构说明

### 代码重构后的结构

**原有问题：**
- `ImageFileController` 中直接处理OSS上传逻辑
- 代码重复，OSS配置分散在各个Controller中
- 不符合单一职责原则

**重构后的结构：**
1. **`SysFileService` 接口**：定义文件上传相关方法
2. **`SysFileServiceImpl` 实现类**：集中处理所有OSS上传逻辑
3. **`ImageFileController`**：专注于图片处理业务逻辑，通过Service调用OSS上传

**优势：**
- ✅ 代码结构更清晰，职责分离
- ✅ OSS配置集中管理，便于维护
- ✅ 支持自定义文件名的上传方法可复用
- ✅ 符合Spring Boot最佳实践

**调用流程：**
```
ImageFileController.compressImageToZip()
    ↓
读取ZIP文件内容为字节数组
    ↓
调用 sysFileService.uploadZipToOssWithCustomName()
    ↓
SysFileServiceImpl.uploadZipToOssWithCustomName()
    ↓
直接使用阿里云OSS SDK上传
    ↓
返回OSS访问URL
```

## 技术特点

### 1. 文件命名策略
- **ZIP文件名**：`{用户指定名称}_{时间戳}.zip`
- **临时目录**：`zip_{时间戳}_{8位随机字符串}/`
- **图片文件名**：`image_{序号}[.{扩展名}]`

### 2. 安全性保障
- **路径随机化**：每次操作使用不同的临时目录
- **文件名清理**：自动清理文件名中的非法字符
- **时间戳唯一性**：确保文件名不会重复

### 3. 性能优化
- **并发处理**：支持多张图片同时下载
- **内存管理**：及时释放图片资源
- **错误恢复**：单张图片失败不影响整体处理

## 注意事项

1. **图片数量限制**：ZIP压缩最多支持100张图片
2. **文件大小**：大图片处理时间较长，建议合理控制图片数量
3. **网络稳定性**：确保图片URL可访问，避免下载失败
4. **临时空间**：确保服务器有足够的临时存储空间

## 错误处理

常见错误及解决方案：

| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 400 | 图片URL数组不能为空 | 检查imageUrls参数 |
| 400 | 图片数量不能超过100张 | 减少图片数量 |
| 400 | 压缩质量必须在0.1到1.0之间 | 检查quality参数值 |
| 400 | 压缩格式只能是jpg或png | 检查format参数值 |
| 400 | 尺寸缩放比例必须在1到100之间 | 检查scale参数值 |
| 400 | 目标KB数必须在1-10000之间 | 检查targetKB参数值 |
| 500 | 图片下载失败 | 检查imageUrl是否可访问 |
| 500 | 无法读取图片文件 | 检查图片格式是否支持 |

## 更新日志

- **v1.3.0** (2024-12-19)
  - 新增图片KB放大功能
  - 智能计算放大倍数，自动达到目标KB数
  - 支持二次放大优化
  - 使用高质量双三次插值算法

- **v1.2.0** (2024-12-19)
  - 新增批量图片ZIP压缩功能
  - 支持自定义ZIP文件名称
  - 增加临时路径随机化，防止文件冲突
  - 自动保留图片文件扩展名
  - 优化错误处理和日志记录

- **v1.1.0** (2024-12-19)
  - 新增压缩格式选择功能（jpg/png）
  - 新增尺寸缩放功能（1-100%）
  - 优化图片处理算法
  - 增强参数验证和错误处理 