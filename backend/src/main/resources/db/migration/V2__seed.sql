INSERT INTO templates (id, title, category, cover_url, description, required_image_count, accepted_mime_types, max_file_size_bytes, result_asset, sort_order) VALUES
('travel-postcard',  '旅行明信片', '创意图片', '/api/assets/covers/travel-postcard.svg',  '上传两张旅行照片，拼成一张复古明信片。',         2, 'image/jpeg,image/png',            5242880, 'travel-postcard.svg',  1),
('pixel-avatar',     '像素头像',   '创意图片', '/api/assets/covers/pixel-avatar.svg',     '上传一张正脸照片，生成 8-bit 风格的像素头像。',   1, 'image/jpeg,image/png,image/webp', 5242880, 'pixel-avatar.svg',     2),
('comic-strip',      '三格漫画',   '创意图片', '/api/assets/covers/comic-strip.svg',      '按顺序上传三张照片，排成一则三格漫画。',         3, 'image/jpeg,image/png',            5242880, 'comic-strip.svg',      3),
('spring-festival',  '新春贺卡',   '节日祝福', '/api/assets/covers/spring-festival.svg',  '上传一张全家福，生成带祝福语的新春贺卡。',       1, 'image/jpeg,image/png',            2097152, 'spring-festival.svg',  4),
('christmas-hug',    '圣诞合影',   '节日祝福', '/api/assets/covers/christmas-hug.svg',    '上传两张人物照片，合成一张圣诞节合影。',         2, 'image/jpeg,image/png,image/webp', 5242880, 'christmas-hug.svg',    5),
('pet-sticker',      '萌宠贴纸',   '萌宠',     '/api/assets/covers/pet-sticker.svg',      '上传一张宠物照片，生成白边描边的表情贴纸。',     1, 'image/jpeg,image/png,image/webp', 5242880, 'pet-sticker.svg',      6),
('pet-oil-painting', '宠物油画像', '萌宠',     '/api/assets/covers/pet-oil-painting.svg', '上传一张宠物照片，生成古典油画风格肖像。',       1, 'image/jpeg,image/png',            2097152, 'pet-oil-painting.svg', 7),
('couple-portrait',  '情侣写真',   '写真婚礼', '/api/assets/covers/couple-portrait.svg',  '上传两人各自的照片，生成一张情侣写真。',         2, 'image/jpeg,image/png,image/webp', 5242880, 'couple-portrait.svg',  8),
('wedding-album',    '婚礼相册',   '写真婚礼', '/api/assets/covers/wedding-album.svg',    '上传三张婚礼照片，排版成一页婚礼相册。',         3, 'image/jpeg,image/png',            5242880, 'wedding-album.svg',    9);

-- 演示账号，密码均为 Demo@123456（BCrypt cost=10）
INSERT INTO users (email, password_hash, display_name, created_at) VALUES
('demo1@example.com', '$2a$10$MXa8Yyt0YcIkQkg/Y8GqgenYwfsI60E7OYKOy0yacbUIB1G0QMaNG', '演示用户一', '2026-01-01 00:00:00.000'),
('demo2@example.com', '$2a$10$MXa8Yyt0YcIkQkg/Y8GqgenYwfsI60E7OYKOy0yacbUIB1G0QMaNG', '演示用户二', '2026-01-01 00:00:00.000');
