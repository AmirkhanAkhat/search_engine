# Search Engine

## Introduction  
This project is a full-featured search engine that indexes and searches web pages. It utilizes lemmatization to improve query understanding and match user input to relevant indexed content. The search results are ranked by relevance, frequency, and context of found terms, and each result includes a title, URL, highlighted text snippet, and a score.

## Problem Statement  
Finding relevant information across websites can be inefficient due to variations in word forms and lack of contextual ranking. This project addresses the need for a smarter search engine that understands the linguistic structure of queries using lemmatization and returns context-aware, ranked results.

## Objectives  
- To provide an efficient and accurate search experience across indexed websites.  
- To process and understand user queries using morphological analysis.  
- To deliver search results with relevance scores and highlighted snippets.  
- To allow both global and site-specific search functionality.

## Technology Stack  
- **Backend:** Java 17, Spring Boot (REST, Web, JPA), Hibernate  
- **Database:** MySQL  
- **Libraries:**  
  - LuceneMorphology (lemmatization)  
  - Jsoup (HTML parsing)  
- **Build Tool:** Maven  

## Installation Instructions

### 1. Clone the repository  
```bash
git clone https://github.com/AmirkhanAkhat/search_engine.git
```

### 2. Configure the database  
- Create the database:  
```sql
CREATE DATABASE search_engine;
```
- Update `src/main/resources/application.properties` with your MySQL credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/search_engine
spring.datasource.username=your_login
spring.datasource.password=your_password
```

### 3. Build and run the application  
Make sure JDK 17, Maven, and MySQL Server are installed. Then run:
```bash
mvn clean install
mvn spring-boot:run
```

The application will be accessible at:  
`http://localhost:8080`

## Usage Guide  
You can perform a search using the endpoint:  
```
GET /api/search
```
With the following query parameters:  
- `query` (required): the search term(s)  
- `site` (optional): filter by a specific site  
- `offset` (optional): result offset for pagination  
- `limit` (optional): number of results to return  

**Example:**  
```
http://localhost:8080/api/search?query=java&site=example.com&offset=0&limit=10
```

Each result includes:
- `URL` of the matching page  
- `Title` of the page  
- Snippet with **highlighted lemmas**  
- Relevance score (relative)

## Testing  
No automated test suite included in this version. Manual testing can be performed using tools like Postman or CURL against the API endpoints.

## Known Issues / Limitations  
- Currently supports only Russian lemmatization (LuceneMorphology).  
- No admin interface for managing sites.  
- No UI/frontend provided — API only.

## References  
- LuceneMorphology 
- [Jsoup HTML Parser](https://jsoup.org/)  
- Spring Boot Documentation  
- MySQL 8.0 Documentation

## Team Members  
- Amirkhan Akhat, 230103090, 14-P


