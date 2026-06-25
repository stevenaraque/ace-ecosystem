package sena.adso.ace_backend.xp.controller

import com.ace.shared.constants.XpConstants
import com.ace.shared.dto.XpFormulaDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.xp.service.FormulaService

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/xp")
class XpController(
    private val formulaService: FormulaService
) {

    @GetMapping("/formulas")
    fun getActiveFormulas(): ResponseEntity<List<XpFormulaDto>> {
        logger.info { "Fetching active XP formulas" }
        val (formulas, maxVersion) = formulaService.getActiveFormulas()

        return ResponseEntity.ok()
            .header(XpConstants.FORMULA_VERSION_HEADER, maxVersion.toString())
            .body(formulas)
    }
}