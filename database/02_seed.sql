/* =====================================================================
   DỮ LIỆU MẪU cho database CineManagementSystem: RoomType, SeatType, Genre, Movie, MovieGenre.
   Chạy SAU 01_schema.sql.
   Giữ nguyên ID cũ để MovieGenre khớp. Chạy trên bảng đang trống; nếu đã có dữ liệu
   ở bảng nào thì bảng đó được bỏ qua, không nhập trùng.
   ===================================================================== */
USE CineManagementSystem;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;   -- lỗi ở bất kỳ dòng nào thì huỷ toàn bộ
BEGIN TRANSACTION;

/* ----------------------------- Genre ----------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.Genre)
BEGIN
    SET IDENTITY_INSERT dbo.Genre ON;
    INSERT INTO dbo.Genre (GenreID, Name, Description) VALUES
    (1,  N'Hành động',   N'Phim tràn ngập các cảnh rượt đuổi, võ thuật, kỹ xảo đỉnh cao và tiết tấu nhanh, dồn dập.'),
    (2,  N'Hài',         N'Phim mang đến yếu tố giải trí, tiếng cười nhẹ nhàng qua các tình huống dí dỏm, hóm hỉnh.'),
    (3,  N'Kinh dị',     N'Phim tạo cảm giác sợ hãi, hồi hộp thông qua các yếu tố ma mị, giật gân hoặc hiện tượng siêu nhiên.'),
    (5,  N'Tâm lý',      N'Phim đi sâu vào diễn biến tâm lý phức tạp, các góc khuất và suy nghĩ ẩn sâu của con người.'),
    (6,  N'Sci-Fic',     N'Phim khai thác thế giới tương lai, công nghệ cao, vũ trụ hoặc du hành thời gian.'),
    (7,  N'Anime',       N'Phim hoạt hình vui nhộn'),
    (8,  N'Lãng Mạn',    N'Phim tập trung vào câu chuyện tình yêu ngọt ngào, rung động và cảm xúc đôi lứa.'),
    (9,  N'Chính Kịch',  N'Phim tập trung khai thác sâu sắc vào xung đột nội tâm, cảm xúc và cuộc sống thực tế của nhân vật.'),
    (10, N'Cổ Trang',    N'Phim lấy bối cảnh lịch sử, thời phong kiến với trang phục và lễ nghi mang đậm màu sắc sắc xưa.'),
    (11, N'Phiêu Lưu',   N'Phim đưa nhân vật vào những chuyến đi thám hiểm vùng đất mới đầy kịch tính và thử thách.'),
    (12, N'Ca Nhạc',     N'Phim xoay quanh âm nhạc, cuộc đời nghệ sĩ hoặc lồng ghép các tiết mục ca vũ kịch.'),
    (13, N'Kỳ Ảo',       N'Phim đưa người xem vào thế giới tưởng tượng đầy phép thuật, sinh vật huyền bí và siêu năng lực.'),
    (14, N'Gia Đình',    N'Phim khai thác tình cảm, mối quan hệ và những câu chuyện sinh hoạt đời thường giữa các thành viên trong gia đình.'),
    (15, N'Thể Thao',    N'Phim tôn vinh tinh thần đồng đội, sự nỗ lực và vinh quang trong các môn thi đấu thể thao.'),
    (16, N'Học Đường',   N'Phim khai thác chủ đề tuổi trẻ, tình bạn, tình yêu và những câu chuyện xoay quanh môi trường trường học.'),
    (17, N'Hình Sự',     N'Phim xoay quanh các vụ án phá án, điều tra tội phạm và cuộc đấu trí giữa cảnh sát với tội phạm.'),
    (18, N'Thần Thoại',  N'Phim dựa trên các truyền thuyết, điển tích cổ xưa về các vị thần và sức mạnh tối cao.'),
    (19, N'Chiến tranh', N'Phim tái hiện các cuộc bối cảnh xung đột vũ trang, sự khốc liệt của chiến trường và số phận con người.');
    SET IDENTITY_INSERT dbo.Genre OFF;
END

/* ----------------------------- Movie -----------------------------
   Cột: MovieID, Title, Description, Director, CastMembers, Language,
        Duration, ReleaseDate, AgeRating, Status, CreatedAt
   (PosterURL, TrailerURL để NULL - cập nhật sau ở màn Poster và trailer) */
IF NOT EXISTS (SELECT 1 FROM dbo.Movie)
BEGIN
    SET IDENTITY_INSERT dbo.Movie ON;
    INSERT INTO dbo.Movie (MovieID, Title, Description, Director, CastMembers, Language,
                           Duration, ReleaseDate, AgeRating, Status, CreatedAt) VALUES
    (1, N'Your Name',
        N'Mitsuha, cô gái sống ở vùng quê, và Taki, chàng trai ở Tokyo, bất ngờ hoán đổi thân xác một cách bí ẩn. Khi cố gắng tìm kiếm nhau, cả hai dần khám phá ra sợi dây định mệnh vượt qua không gian và thời gian.',
        N'Nguyễn Văn Mười', N'Taki Tachibana, Mitsuha Miyamizu', N'Tiếng Việt', 112, '2026-10-03', 'T13', 'Now Showing', '2026-10-13 19:38:11.2310764'),
    (2, N'Violet Evergarden : Hồi Ức Không Quên',
        N'Sau chiến tranh, Violet Evergarden tiếp tục hành trình viết những bức thư thay lời muốn nói, giúp mọi người gửi gắm cảm xúc sâu kín nhất. Trong hành trình ấy, cô vẫn luôn tìm kiếm ý nghĩa của tình yêu và lời hứa cuối cùng với người mà cô trân quý nhất.',
        N'Taichi Ishidate', N'Violet Evergarden, Gilbert Bougainvillea, Claudia Hodgins, Cattleya Baudelaire, Benedict Blue', N'Tiếng Việt', 136, '2026-10-08', 'T16', 'Now Showing', '2026-10-13 19:38:11.2310764'),
    (3, N'Iron Man 3',
        N'Sau những sự kiện ở New York, Tony Stark phải đối mặt với những ám ảnh và nỗi sợ bên trong khi một kẻ thù bí ẩn mang tên Mandarin đe dọa phá hủy thế giới của anh. Mất đi lợi thế công nghệ, Tony buộc phải dựa vào trí tuệ và bản lĩnh để bảo vệ những người mình yêu thương.',
        N'Shane Black', N'Robert Downey Jr, Gwyneth Paltrow, Guy Pearce', N'Tiếng Anh', 120, '2026-08-14', 'T18', 'End of Screening', '2026-10-13 19:38:11.2310764'),
    (4, N'21 Jump Street',
        N'Hai cảnh sát trẻ Schmidt và Jenko được cử trở lại trường trung học dưới vỏ bọc học sinh để triệt phá đường dây ma túy. Thế nhưng, nhiệm vụ tưởng chừng đơn giản nhanh chóng biến thành hàng loạt tình huống dở khóc dở cười, thử thách tình bạn và khả năng phá án của cả hai.',
        N'Christopher Miller', N'Jonah Hill, Channing Tatum, Brie Larson, Dave Franco', N'Tiếng Việt', 120, '2017-11-16', 'T18', 'Coming Soon', '2026-10-14 23:13:12.6590966'),
    (5, N'Our Beloved Summer',
        N'Choi Ung và Kook Yeon-su, hai người từng có mối tình thanh xuân đầy cảm xúc, bất ngờ gặp lại nhau khi bộ phim tài liệu về thời trung học của họ trở nên nổi tiếng. Những ký ức, hiểu lầm và tình cảm chưa từng phai nhạt dần được khơi lại, buộc cả hai đối diện với quá khứ và những điều còn dang dở trong trái tim mình.',
        N'Kim Yoon-jin', N'Choi Woo-shik, Kim Da-mi, Roh Jeong-eui', N'Tiếng Hàn', 123, '2026-06-18', 'T13', 'Coming Soon', '2026-10-14 23:16:32.5595421'),
    (6, N'Queen of Tears',
        N'Baek Hyun-woo, chàng rể xuất thân bình thường, và Hong Hae-in, nữ thừa kế tập đoàn tài phiệt, đang trải qua cuộc hôn nhân bên bờ tan vỡ. Khi biến cố bất ngờ ập đến, cả hai dần tìm lại tình yêu đã đánh mất, học cách thấu hiểu và cùng nhau vượt qua những thử thách nghiệt ngã của cuộc đời.',
        N'Jang Young-woo', N'Kim Soo-hyun, Kim Ji-won, Park Sung-hoon, Kwak Dong-yeon', N'Tiếng Hàn', 143, '2026-10-14', 'T13', 'Now Showing', '2026-10-14 23:19:38.1591566'),
    (7, N'Na Tra 2 : Ma Đồng Náo Hải',
        N'Na Tra 2: Ma Đồng Náo Hải kể về hành trình của Na Tra và Ngao Bính sau khi thân xác bị hủy hoại bởi thiên kiếp. Đứng trước những âm mưu và thử thách mới, cả hai phải cùng nhau đối mặt với số phận, chiến đấu để bảo vệ những người mình yêu thương và viết lại định mệnh của chính mình.',
        N'Yang Yu', N'Ne Zha, Ao Bing, Taiyi Zhenren', N'Tiếng Trung', 123, '2026-03-12', 'P', 'Now Showing', '2026-10-14 23:21:50.6740679'),
    (8, N'You Are the Apple of My Eye',
        N'Câu chuyện kể về tuổi thanh xuân của Kha Cảnh Đằng, một chàng trai nghịch ngợm, và Thẩm Giai Nghi, cô nữ sinh ưu tú luôn khiến cậu ngưỡng mộ. Từ những ngày tháng học trò đầy tiếng cười đến những rung động đầu đời, cả hai cùng trải qua một mối tình trong sáng nhưng dang dở, để rồi nhận ra rằng có những người dù không thể đi cùng ta đến cuối con đường, vẫn mãi là ký ức đẹp nhất của thanh xuân.',
        N'Giddens Ko', N'Kai Ko, Michelle Chen, Shao-Wen Hao', N'Tiếng Trung', 109, '2025-06-14', 'T13', 'Coming Soon', '2026-10-14 23:30:25.6591600'),
    (9, N'La La Land',
        N'Giữa Los Angeles đầy mộng mơ, Sebastian, một nhạc công jazz đầy hoài bão, và Mia, cô diễn viên trẻ khao khát thành công, tình cờ gặp gỡ và đem lòng yêu nhau. Cùng chia sẻ những ước mơ và khát vọng, họ phải đối mặt với lựa chọn giữa tình yêu và sự nghiệp, để rồi nhận ra rằng đôi khi, người ta có thể thay đổi cuộc đời nhau mà không nhất thiết phải đi cùng nhau đến cuối con đường.',
        N'La La Land', N'Ryan Gosling, Emma Stone, John Legend', N'Tiếng Anh', 128, '2025-02-14', 'K', 'Coming Soon', '2026-10-14 23:33:18.3135840'),
    (10, N'Weathering with You',
        N'Hodaka, một chàng trai bỏ nhà lên Tokyo, tình cờ gặp Hina, cô gái sở hữu khả năng kỳ diệu có thể xua tan mưa và mang ánh nắng đến bất cứ nơi nào. Giữa thành phố chìm trong những cơn mưa bất tận, cả hai dần gắn bó và đứng trước lựa chọn khó khăn giữa tình yêu, số phận và sự cân bằng của thế giới.',
        N'Makoto Shinkai', N'Hodaka Morishima, Hina Amano, Natsumi Suga', N'Tiếng Nhật', 114, '2026-10-02', 'P', 'End of Screening', '2026-10-14 23:35:26.3126106'),
    (11, N'Chainsaw Man – The Movie: Reze Arc',
        N'Denji, chàng thợ săn quỷ mang trái tim của Quỷ Cưa, gặp gỡ Reze, một cô gái bí ẩn khiến trái tim anh lần đầu rung động. Nhưng đằng sau những khoảnh khắc ngọt ngào là một cuộc chiến đẫm máu giữa tình yêu, sự phản bội và những bí mật chết người.',
        N'Tatsuya Yoshihara', N'Denji, Reze, Makima, Aki Hayakawa', N'Tiếng Nhật', 132, '2025-07-14', 'T13', 'Coming Soon', '2026-10-14 23:39:48.4295723'),
    (12, N'Mai',
        N'Sau một cuộc gặp gỡ tình cờ, một chàng trai trẻ phát hiện mình có thể nhìn thấy những khoảnh khắc cuối cùng trong cuộc đời của người khác. Khi tình yêu và những bí mật dần hé lộ, anh phải đối mặt với những lựa chọn khó khăn giữa số phận, sự mất mát và cơ hội thứ hai trong cuộc đời.',
        N'Trấn Thành', N'Phương Anh Đào, Tuấn Trần, Hồng Đào, Trấn Thành', N'Tiếng Việt', 131, '2025-05-28', 'T16', 'Coming Soon', '2026-10-14 23:50:31.6108699'),
    (13, N'My Love',
        N'Suốt 15 năm, Châu Tiêu Tề chưa bao giờ quên Hạ Vũ, cô gái đã bước vào cuộc đời anh từ những năm tháng thanh xuân. Từ tình bạn, rung động đầu đời đến những lần lỡ hẹn đầy tiếc nuối, cả hai trải qua một hành trình tình yêu dài đằng đẵng, nơi thời gian có thể thay đổi mọi thứ, nhưng có những người mãi mãi không thuộc về nhau.',
        N'Han Tian', N'Greg Hsu, Zhang Ruonan', N'Tiếng Trung', 115, '2024-06-13', 'T13', 'Coming Soon', '2026-10-14 23:53:09.9240846'),
    (14, N'Em chưa 18',
        N'Tuổi 17 nổi loạn, Linh Linh bước vào một mối tình bí mật với Trương Nhất Sơn, chàng trai trưởng thành hơn cô. Giữa những rung động đầu đời, sự bồng bột và những lựa chọn sai lầm, cả hai phải đối mặt với những hậu quả khó lường khi tình yêu tuổi trẻ vượt qua giới hạn.',
        N'Lê Thanh Sơn', N'Kaity Nguyễn, Kiều Minh Tuấn, Will, Châu Bùi', N'Tiếng Việt', 120, '2022-06-14', 'T16', 'Coming Soon', '2026-10-14 23:55:40.2742621'),
    (15, N'Mưa đỏ',
        N'Lấy bối cảnh 81 ngày đêm chiến đấu bảo vệ Thành cổ Quảng Trị năm 1972, *Mưa Đỏ* tái hiện cuộc chiến khốc liệt qua câu chuyện về những người lính trẻ sẵn sàng hy sinh tuổi thanh xuân vì độc lập, tự do của Tổ quốc. Giữa bom đạn và mất mát, tình đồng đội, lòng yêu nước và khát vọng hòa bình vẫn tỏa sáng đầy xúc động.',
        N'Đặng Thái Huyền', N'Steven Nguyễn, Đỗ Nhật Hoàng, Lê Hạ Anh, Phương Nam, Lâm Thanh Nhã', N'Tiếng Việt', 124, '2025-09-02', 'P', 'Coming Soon', '2026-10-14 23:58:21.1686559'),
    (16, N'A Silent Voice',
        N'Shoya Ishida từng bắt nạt Shoko Nishimiya, cô bạn khiếm thính mới chuyển đến trường, khiến cô phải rời đi trong đau khổ. Nhiều năm sau, Shoya sống trong sự day dứt và cô độc, quyết tâm tìm gặp Shoko để chuộc lại lỗi lầm. Qua hành trình ấy, cả hai dần học cách tha thứ, đối diện với quá khứ và tìm thấy ý nghĩa của sự kết nối giữa con người.',
        N'Naoko Yamada', N'Shōya Ishida, Shōko Nishimiya, Naoka Ueno, Miyako Ishida', N'Tiếng Nhật', 130, '2016-06-15', 'P', 'Coming Soon', '2026-10-15 00:02:46.4481769'),
    (17, N'Forrest Gump',
        N'Forrest Gump, một chàng trai có chỉ số IQ thấp nhưng sở hữu trái tim chân thành và nghị lực phi thường, vô tình trở thành nhân chứng của nhiều sự kiện lịch sử nước Mỹ. Trên hành trình cuộc đời đầy bất ngờ, anh trải qua tình bạn, chiến tranh, thành công và mất mát, nhưng chưa bao giờ ngừng yêu thương Jenny – người con gái anh luôn dành trọn trái tim.',
        N'Robert Zemeckis', N'Tom Hanks, Gary Sinise, Mykelti Williamson, Sally Field', N'Tiếng Anh', 142, '1994-06-15', 'P', 'End of Screening', '2026-10-15 00:05:38.3040428'),
    (18, N'Chàng trai năm ấy',
        N'Dựa trên câu chuyện có thật về Wan Wan, bộ phim kể về những năm tháng thanh xuân của Tô Hữu Bằng, một chàng trai đem lòng yêu cô bạn cùng lớp Thẩm Giai Nghi. Từ những rung động đầu đời đến những tiếc nuối khi trưởng thành, câu chuyện khắc họa một tình yêu trong sáng nhưng dang dở, nơi có những người dù không thể đi cùng nhau đến cuối con đường vẫn mãi là ký ức đẹp nhất của tuổi trẻ.',
        N'Nguyễn Quang Huy', N'Sơn Tùng M-TP, Hari Won, Ngô Kiến Huy, Phạm Quỳnh Anh', N'Tiếng Việt', 119, '2026-10-08', 'P', 'Now Showing', '2026-10-15 00:18:10.2058344'),
    (19, N'Finding Nemo',
        N'Sau khi con trai Nemo bị bắt khỏi rạn san hô và đưa đến một bể cá ở Sydney, chú cá hề Marlin vượt qua đại dương rộng lớn để tìm con. Đồng hành cùng cô cá đãng trí Dory, Marlin phải đối mặt với vô vàn hiểm nguy và dần học cách tin tưởng, buông bỏ nỗi sợ để con trai có thể trưởng thành.',
        N'Andrew Stanton', N'Nemo, Dory, Gill', N'Tiếng Anh', 100, '2017-06-15', 'P', 'Coming Soon', '2026-10-15 00:20:52.3312844'),
    (20, N'Spider-Man: No Way Home',
        N'Sau khi danh tính Spider-Man bị tiết lộ, Peter Parker tìm đến Doctor Strange để nhờ phép thuật khiến mọi người quên đi sự thật. Nhưng khi câu thần chú gặp trục trặc, những kẻ thù từ các vũ trụ khác bắt đầu xuất hiện, đẩy Peter vào cuộc chiến buộc anh phải lựa chọn giữa cuộc sống cá nhân và trách nhiệm của một siêu anh hùng.',
        N'Jon Watts', N'Tom Holland, Andrew Garfield, Tobey Maguire, Zendaya', N'Tiếng Anh', 148, '2022-06-15', 'P', 'Coming Soon', '2026-10-15 00:22:55.1402962'),
    (21, N'Sword Art Online: Ordinal Scale',
        N'Trong thế giới của Ordinal Scale, một trò chơi thực tế tăng cường mới nổi nhanh chóng thu hút người chơi trên khắp Nhật Bản. Kirito và Asuna dần phát hiện những bí mật nguy hiểm ẩn sau trò chơi, buộc cả hai phải chiến đấu để bảo vệ ký ức, những người thân yêu và sự thật về quá khứ của Aincrad.',
        N'Tomohiko Itō.', N'Kirigaza Kazuto, Yuuki Asuna, Yui, Yuna Shigemura', N'Tiếng Nhật', 119, '2017-02-15', 'P', 'Now Showing', '2026-10-15 00:26:20.1178224'),
    (22, N'Kimi wo Aishita Hitori no Boku e',
        N'Trong một thế giới nơi các vũ trụ song song tồn tại, Koyomi Takasaki gặp Shiori Satō và cả hai dần nảy sinh tình cảm với nhau. Khi tình yêu đứng trước những lựa chọn giữa các thực tại khác nhau, Koyomi phải tìm cách vượt qua ranh giới giữa các thế giới để bảo vệ người con gái mình yêu.',
        N'Kenichi Kasai', N'Koyomi Hidaka, Shiori Satō, Kazune Takigawa', N'Tiếng Nhật', 98, '2025-07-15', 'P', 'Coming Soon', '2026-10-15 00:30:07.1592771'),
    (23, N'Ronaldo',
        N'Bộ phim tài liệu theo chân Cristiano Ronaldo trong cuộc sống và sự nghiệp đỉnh cao, hé lộ những góc nhìn chân thực về hành trình trở thành một trong những cầu thủ vĩ đại nhất lịch sử bóng đá. Qua những khoảnh khắc trên sân cỏ và cuộc sống đời thường bên gia đình, bộ phim khắc họa tham vọng, sự hy sinh và con người phía sau ánh hào quang của CR7.',
        N'Anthony Wonke', N'Cristiano Ronaldo', N'Tiếng Anh', 92, '2026-10-08', 'P', 'Now Showing', '2026-10-15 01:14:14.0063737'),
    (24, N'Thám Tử Lừng Danh Conan Movie 23: Cú Đấm Sapphire Xanh',
        N'Trong *Thám Tử Lừng Danh Conan: Cú Đấm Sapphire Xanh*, Conan bị cuốn vào một vụ án bí ẩn tại Singapore liên quan đến viên sapphire xanh lớn nhất thế giới. Khi Kaito Kid bị nghi ngờ là hung thủ của một vụ giết người, Conan phải hợp tác bất đắc dĩ với siêu đạo chích để vén màn âm mưu nguy hiểm, đồng thời đối đầu với võ sĩ karate mạnh nhất thế giới – Makoto Kyogoku.',
        N'Nagaoka Chika', N'Edogawa Conan, Mouri Ran, Mouri Kogoro, Makoto Kyogoku, Sonoko Suzuki', N'Tiếng Nhật', 120, '2026-10-07', 'P', 'Coming Soon', '2026-10-15 01:17:11.5974520'),
    (25, N'Mãn Giang Hồng',
        N'Lấy bối cảnh thời Nam Tống, *Mãn Giang Hồng* xoay quanh cuộc điều tra vụ án sát hại sứ thần Kim quốc ngay trước cuộc đàm phán chính trị quan trọng. Trong vòng xoáy của những âm mưu, phản bội và bí mật chồng chéo, một nhóm người phải chạy đua với thời gian để tìm ra sự thật, nơi lòng trung thành và vận mệnh đất nước được đặt lên bàn cân.',
        N'Trương Nghệ Mưu', N'Thẩm Đằng, Dịch Dương Thiên Tỉ, Lôi Giai Âm, Hứa Tĩnh Nhã', N'Tiếng Trung', 157, '2026-10-08', 'P', 'Coming Soon', '2026-10-15 01:21:13.3842212'),
    (26, N'Howl''s Moving Castle',
        N'Sophie, một cô gái trẻ bị phù thủy nguyền rủa thành bà lão, tình cờ bước vào tòa lâu đài di động kỳ bí của pháp sư Howl. Giữa thế giới phép thuật và cuộc chiến đang tàn phá vương quốc, cả hai dần xích lại gần nhau, khám phá ý nghĩa của tình yêu, lòng dũng cảm và sự tự do để sống đúng với con người mình.',
        N'Hayao Miyazaki', N'Sophie Hatter, Howl Jenkins Pendragon, Calcifer, Markl', N'Tiếng Nhật', 119, '2026-10-09', 'P', 'Now Showing', '2026-10-15 15:52:59.0876965');
    SET IDENTITY_INSERT dbo.Movie OFF;
END

/* --------------------------- MovieGenre --------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.MovieGenre)
BEGIN
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES
    (1,2),(1,7),(1,8),(1,13),(1,16),
    (2,5),(2,7),(2,8),
    (3,1),(3,2),(3,5),(3,6),
    (4,1),(4,2),
    (5,2),(5,8),(5,9),(5,14),(5,16),
    (6,5),(6,8),(6,9),(6,14),
    (7,1),(7,7),(7,13),(7,14),(7,18),
    (8,2),(8,8),(8,16),
    (9,8),(9,12),
    (10,2),(10,7),(10,8),(10,13),(10,16),
    (11,1),(11,3),(11,5),(11,7),(11,8),(11,13),
    (12,2),(12,8),(12,14),
    (13,2),(13,8),
    (14,2),(14,8),(14,9),(14,14),(14,16),
    (15,9),(15,19),
    (16,2),(16,5),(16,7),(16,8),(16,16),
    (17,2),(17,8),
    (18,2),(18,5),(18,8),(18,14),
    (19,2),(19,7),(19,11),(19,13),(19,14),
    (20,1),(20,5),(20,13),
    (21,5),(21,6),(21,7),(21,8),(21,13),(21,16),
    (22,2),(22,5),(22,6),(22,8),(22,13),
    (23,14),(23,15),
    (24,1),(24,5),(24,6),(24,7),(24,17),
    (25,1),(25,9),(25,10),(25,19),
    (26,2),(26,5),(26,7),(26,8),(26,11),(26,13);
END

COMMIT TRANSACTION;
GO

/* ------------------------- RoomType, SeatType ------------------------- */
/* Tên SeatType phải chứa chữ "Sweetbox" thì sơ đồ ghế mới áp luật ghế đôi (đặt theo cặp). */
INSERT INTO dbo.RoomType (Name, Surcharge, Description)
SELECT v.Name, v.Surcharge, v.Description
FROM (VALUES (N'2D', 0, N'Tiêu chuẩn'), (N'3D', 30000, N'Kính 3D'), (N'IMAX', 70000, N'Màn lớn'))
     AS v(Name, Surcharge, Description)
WHERE NOT EXISTS (SELECT 1 FROM dbo.RoomType r WHERE r.Name = v.Name);

INSERT INTO dbo.SeatType (Name, Surcharge, Color)
SELECT v.Name, v.Surcharge, v.Color
FROM (VALUES (N'Standard', 0, '#7d8299'), (N'VIP', 20000, '#b7791f'), (N'Sweetbox', 50000, '#a3263b'))
     AS v(Name, Surcharge, Color)
WHERE NOT EXISTS (SELECT 1 FROM dbo.SeatType s WHERE s.Name = v.Name);
GO

/* Kiểm tra: RoomType = 3, SeatType = 3, Genre = 18, Movie = 26, MovieGenre = 103 */
SELECT (SELECT COUNT(*) FROM dbo.RoomType)   AS SoLoaiPhong,
       (SELECT COUNT(*) FROM dbo.SeatType)   AS SoLoaiGhe,
       (SELECT COUNT(*) FROM dbo.Genre)      AS SoTheLoai,
       (SELECT COUNT(*) FROM dbo.Movie)      AS SoPhim,
       (SELECT COUNT(*) FROM dbo.MovieGenre) AS SoLienKet;
GO
