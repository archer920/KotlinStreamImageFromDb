package com.stonesoupprogramming.streamimage

import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import javax.persistence.*
import javax.transaction.Transactional
import javax.xml.bind.DatatypeConverter

@SpringBootApplication
class StreamImageDbApplication

fun main(args: Array<String>) {
    SpringApplication.run(StreamImageDbApplication::class.java, *args)
}

@Entity
data class PersistedImage(@field: Id @field: GeneratedValue var id : Long = 0,
                          //The bytes field needs to be marked as @Lob for Large Object Binary
                          @field: Lob var bytes : ByteArray? = null,
                          var mime : String = ""){

    fun toStreamingURI() : String {
        //We need to encode the byte array into a base64 String for the browser
        val base64 = DatatypeConverter.printBase64Binary(bytes)

        //Now just return a data string. The Browser will know what to do with it
        return "data:$mime;base64,$base64"
    }
}

//This is a Kotlin extension function that turns a MultipartFile into a PersistedImage
fun MultipartFile.toPersistedImage() = PersistedImage(bytes = this.bytes, mime = this.contentType)

@Configuration
class DataConfig {

    @Bean
    fun sessionFactory(@Autowired entityManagerFactory: EntityManagerFactory) :
             SessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
}

@Repository
class ImageRepository(@Autowired private val sessionFactory: SessionFactory){

    fun save(persistedImage: PersistedImage) {
        sessionFactory.currentSession.saveOrUpdate(persistedImage)
    }

    fun loadAll() = sessionFactory.currentSession.createCriteria(PersistedImage::class.java).list() as List<PersistedImage>
}

@Transactional
@Service
class ImageService(@Autowired private val imageRepository: ImageRepository){

    fun save(persistedImage: PersistedImage) {
        imageRepository.save(persistedImage)
    }

    fun loadAll() = imageRepository.loadAll()
}

@Controller
@RequestMapping("/")
class IndexController(@Autowired private val imageService: ImageService){

    @RequestMapping(method = arrayOf(RequestMethod.GET))
    fun doGet(model : Model) : String {
        model.addAttribute("images", imageService.loadAll())
        return "index"
    }

    @RequestMapping(method = arrayOf(RequestMethod.POST))
    fun doPost(
            //Grab the uploaded image from the form
            @RequestPart("image") multiPartFile : MultipartFile,
               model : Model) : String {
        //Save the image file
        imageService.save(multiPartFile.toPersistedImage())
        model.addAttribute("images", imageService.loadAll())
        return "index"
    }
}
