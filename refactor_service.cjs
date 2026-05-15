const fs = require('fs');
const path = require('path');

function walk(dir, callback) {
    fs.readdirSync(dir).forEach(f => {
        let dirPath = path.join(dir, f);
        let isDirectory = fs.statSync(dirPath).isDirectory();
        if (isDirectory) walk(dirPath, callback);
        else callback(path.join(dir, f));
    });
}

function replaceInFile(filePath, replacements) {
    if (!fs.existsSync(filePath)) return;
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;
    for (const [search, replace] of replacements) {
        if (search instanceof RegExp) {
            content = content.replace(search, replace);
        } else {
            content = content.split(search).join(replace);
        }
    }
    if (content !== original) {
        fs.writeFileSync(filePath, content);
    }
}

// 1. Rename files
const renames = [
    ['salon-back/src/main/java/com/cristiane/salon/models/service/entity/Service.java', 'salon-back/src/main/java/com/cristiane/salon/models/service/entity/SalonService.java'],
    ['salon-back/src/main/java/com/cristiane/salon/models/service/repository/ServiceRepository.java', 'salon-back/src/main/java/com/cristiane/salon/models/service/repository/SalonServiceRepository.java'],
    ['salon-back/src/main/java/com/cristiane/salon/models/service/dto/ServiceRequest.java', 'salon-back/src/main/java/com/cristiane/salon/models/service/dto/SalonServiceRequest.java'],
    ['salon-back/src/main/java/com/cristiane/salon/models/service/dto/ServiceResponse.java', 'salon-back/src/main/java/com/cristiane/salon/models/service/dto/SalonServiceResponse.java'],
    ['salon-back/src/main/java/com/cristiane/salon/models/service/service/ServiceService.java', 'salon-back/src/main/java/com/cristiane/salon/models/service/service/SalonServiceManager.java'], // avoiding SalonServiceService which sounds silly
    ['salon-back/src/main/java/com/cristiane/salon/controller/ServiceController.java', 'salon-back/src/main/java/com/cristiane/salon/controller/SalonServiceController.java']
];

renames.forEach(([src, dest]) => {
    if (fs.existsSync(src)) {
        fs.renameSync(src, dest);
    }
});

// 2. Replacements
const commonReplacements = [
    ["import com.cristiane.salon.models.service.entity.Service;", "import com.cristiane.salon.models.service.entity.SalonService;"],
    ["import com.cristiane.salon.models.service.repository.ServiceRepository;", "import com.cristiane.salon.models.service.repository.SalonServiceRepository;"],
    ["import com.cristiane.salon.models.service.dto.ServiceRequest;", "import com.cristiane.salon.models.service.dto.SalonServiceRequest;"],
    ["import com.cristiane.salon.models.service.dto.ServiceResponse;", "import com.cristiane.salon.models.service.dto.SalonServiceResponse;"],
    ["import com.cristiane.salon.models.service.service.ServiceService;", "import com.cristiane.salon.models.service.service.SalonServiceManager;"],
    ["public class ServiceController", "public class SalonServiceController"],
    ["public class ServiceService", "public class SalonServiceManager"],
    ["public interface ServiceRepository", "public interface SalonServiceRepository"],
    ["public record ServiceRequest", "public record SalonServiceRequest"],
    ["public record ServiceResponse", "public record SalonServiceResponse"],
    ["public class Service {", "public class SalonService {"],
    [" ServiceRepository ", " SalonServiceRepository "],
    [" ServiceService ", " SalonServiceManager "],
    [" ServiceRequest ", " SalonServiceRequest "],
    [" ServiceResponse ", " SalonServiceResponse "],
    [" Service ", " SalonService "],
    ["<Service>", "<SalonService>"],
    ["serviceRepository", "salonServiceRepository"],
    ["serviceService", "salonServiceManager"],
    // Entity specific
    ['@Table(name = "tb_service")', '@Table(name = "tb_salon_service")'],
    // Controller endpoints
    ['ServiceResponse.fromEntity', 'SalonServiceResponse.fromEntity'],
];

walk('salon-back/src/main/java/com/cristiane/salon', (filePath) => {
    if (!filePath.endsWith('.java')) return;
    replaceInFile(filePath, commonReplacements);
});

// 3. Appointment specifics
replaceInFile('salon-back/src/main/java/com/cristiane/salon/models/appointment/entity/Appointment.java', [
    ['private SalonService service;', 'private SalonService salonService;'],
    ['@JoinColumn(name = "service_id", nullable = false)', '@JoinColumn(name = "salon_service_id", nullable = false)']
]);

replaceInFile('salon-back/src/main/java/com/cristiane/salon/models/appointment/service/AppointmentService.java', [
    ['appointment.getService()', 'appointment.getSalonService()'],
    ['appointment.setService(', 'appointment.setSalonService(']
]);

replaceInFile('salon-back/src/main/java/com/cristiane/salon/models/appointment/dto/AppointmentResponse.java', [
    ['appointment.getService()', 'appointment.getSalonService()']
]);

replaceInFile('salon-back/src/main/java/com/cristiane/salon/models/report/service/ReportService.java', [
    ['a.getService().getName()', 'a.getSalonService().getName()']
]);

console.log("Backend refactored");
