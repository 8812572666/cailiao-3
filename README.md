# Material Management System

A modern material data management system based on Spring Boot, providing an intuitive web interface for managing and viewing material information in databases.

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/template/your-template-id)

## ✨ Features

- 🔗 **Database Connection Management** - Support for MySQL database connections
- 📊 **Data Visualization** - Intuitive data table display and statistics
- 🔍 **Smart Search** - Quick data search and filtering
- 📤 **Data Export** - Support for CSV format data export
- 🎨 **Responsive Design** - Adapts to various device screens
- ⚡ **High Performance** - Optimized database connection pool and query performance
- 🔒 **Security & Reliability** - Comprehensive error handling and data validation
- ☁️ **Cloud Deployment Ready** - Support for Railway, Heroku and other cloud platforms

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.2.0, Spring MVC, Spring JDBC
- **Frontend**: Thymeleaf, Bootstrap 5, jQuery
- **Database**: MySQL 8.0+
- **Connection Pool**: HikariCP (High-performance connection pool)
- **Build Tool**: Maven 3.6+
- **Java Version**: JDK 17+
- **Cloud Storage**: Alibaba Cloud OSS

## 🚀 Quick Deployment

### Railway Deployment (Recommended)

1. Click the "Deploy on Railway" button above
2. Connect your GitHub account
3. Configure environment variables (see configuration below)
4. Wait for automatic deployment to complete

### Environment Variables Configuration

Set the following environment variables in Railway:

```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://your-host:3306/your-database?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password

# OSS Configuration (Optional)
OSS_ACCESS_KEY_ID=your-oss-access-key
OSS_ACCESS_KEY_SECRET=your-oss-secret-key
OSS_ENDPOINT=oss-cn-wuhan-lr.aliyuncs.com
OSS_TEXT_BUCKET=your-text-bucket
OSS_IMAGE_BUCKET=your-image-bucket
```

## 📋 Local Development

### System Requirements

1. **Java Development Kit (JDK)**
   - Version: JDK 17 or higher
   - Recommended: OpenJDK 17+ or Oracle JDK 17+

2. **Apache Maven**
   - Version: Maven 3.6+

### Local Startup

```bash
# Clone the project
git clone https://github.com/your-username/material-management-system.git
cd material-management-system

# Compile and run
mvn spring-boot:run
```

Access http://localhost:8080

## 🎯 Features Overview

### 1. Database Management
- Connect to multiple MySQL databases
- Real-time database status monitoring
- Database statistics display

### 2. Table Data Browsing
- Paginated table data browsing
- Table structure information viewing
- Data types and constraints display

### 3. File Management
- Integrated Alibaba Cloud OSS storage
- Image thumbnail preview
- Online text file preview

### 4. Data Export
- CSV format data export
- Customizable export quantity
- Complete column information included

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/material/management/
│   │   ├── MaterialManagementApplication.java  # Main startup class
│   │   ├── config/                            # Configuration classes
│   │   │   ├── DatabaseConfig.java            # Database configuration
│   │   │   └── OssConfig.java                 # OSS configuration
│   │   ├── controller/                        # Controllers
│   │   │   ├── MainController.java            # Page controller
│   │   │   └── ApiController.java             # API controller
│   │   ├── model/                            # Data models
│   │   │   ├── DatabaseInfo.java              # Database info model
│   │   │   ├── TableInfo.java                 # Table info model
│   │   │   └── ColumnInfo.java                # Column info model
│   │   └── service/                          # Business services
│   │       ├── DatabaseService.java           # Database service
│   │       └── OssService.java                # OSS service
│   └── resources/
│       ├── application.yml                    # Application configuration
│       ├── templates/                         # Thymeleaf templates
│       │   ├── index.html                     # Home page
│       │   ├── database-detail.html           # Database detail page
│       │   └── table-data.html                # Table data page
│       └── static/                           # Static resources
│           ├── css/                           # Stylesheets
│           ├── js/                            # JavaScript files
│           └── images/                        # Image resources
├── pom.xml                                   # Maven configuration
├── .gitignore                                # Git ignore rules
└── README.md                                 # Project documentation
```

## 🔧 Configuration

### Database Configuration

The system supports environment variable configuration for easy cloud deployment:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/test}
    username: ${DATABASE_USERNAME:root}
    password: ${DATABASE_PASSWORD:password}
```

### Server Configuration

Supports dynamic port allocation for cloud platforms:

```yaml
server:
  port: ${PORT:8080}  # Railway and other cloud platforms will automatically assign ports
```

### OSS Configuration

Alibaba Cloud OSS configuration with environment variable support:

```yaml
oss:
  access-key-id: ${OSS_ACCESS_KEY_ID:your-access-key}
  access-key-secret: ${OSS_ACCESS_KEY_SECRET:your-secret-key}
  endpoint: ${OSS_ENDPOINT:oss-cn-wuhan-lr.aliyuncs.com}
  text-bucket-name: ${OSS_TEXT_BUCKET:your-text-bucket}
  image-bucket-name: ${OSS_IMAGE_BUCKET:your-image-bucket}
```

## 🚨 Troubleshooting

### Cloud Deployment Issues

1. **Database Connection Failed**
   - Check database whitelist settings to allow Railway IP addresses
   - Verify environment variable configuration is correct
   - Validate database service status and network connectivity

2. **Port Access Issues**
   - Ensure using `${PORT:8080}` configuration in application.yml
   - Check cloud platform port allocation and routing

3. **Build Failed**
   - Check Java version compatibility (requires JDK 17+)
   - Verify Maven dependencies are complete and accessible
   - Check for any compilation errors in the logs

4. **OSS Connection Issues**
   - Verify OSS access keys and endpoint configuration
   - Check OSS bucket permissions and CORS settings
   - Ensure bucket names are correct and accessible

### Local Development Issues

1. **Port Already in Use**
   - Modify port number in application.yml
   - Or stop the program occupying port 8080: `netstat -ano | findstr :8080`

2. **Maven Dependency Download Failed**
   - Check network connection and proxy settings
   - Clean Maven cache: `mvn clean`
   - Try using a different Maven repository mirror

3. **Database Connection Issues**
   - Verify database server is running and accessible
   - Check database credentials and connection string
   - Ensure database driver is included in dependencies

## 🌟 Performance Features

- **Optimized Connection Pool**: HikariCP with custom configuration for high performance
- **Query Optimization**: Prepared statement caching and batch processing
- **Resource Compression**: Gzip compression for web resources
- **Caching Strategy**: Database query result caching for frequently accessed data
- **Pagination Support**: Efficient large dataset handling with pagination

## 🔒 Security Features

- **SQL Injection Prevention**: Parameterized queries and input validation
- **Error Handling**: Comprehensive exception handling without sensitive data exposure
- **Connection Security**: SSL support for database connections
- **Input Validation**: Server-side validation for all user inputs

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

If you encounter any issues while using this system:

1. Check the troubleshooting section above
2. Search existing [Issues](../../issues) for similar problems
3. Create a new Issue with detailed description of your problem

---

**Material Management System** - Making data management simpler and more efficient! 🚀