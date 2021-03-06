import EVENTS from '../../../../constants/artifacts_events.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecuritySamlIntegrationController {

    constructor(SamlDao, ArtifactoryModelSaver, ArtifactoryEventBus) {
        this.samlDao = SamlDao.getInstance();
        this.TOOLTIP = TOOLTIP.admin.security.SAMLSSOSettings;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['saml']);
        this.artifactoryEventBus = ArtifactoryEventBus;
        this._init();
    }

    _init() {
        this.samlDao.get().$promise.then((data) => {
            this.saml = data;
            if (!angular.isDefined(this.saml.noAutoUserCreation)) {
                this.saml.noAutoUserCreation = true;
            }
            this.artifactoryModelSaver.save();
        });
    }

    save() {
        this.samlDao.update(this.saml).$promise.then(()=>{
            this.artifactoryModelSaver.save();
            this.artifactoryEventBus.dispatch(EVENTS.FOOTER_DATA_UPDATED);
        });
    }

    cancel() {
        this.artifactoryModelSaver.ask().then(()=>{
            this._init();
        });
    }
    canSave() {
        return this.samlForm.$valid;
    }
}