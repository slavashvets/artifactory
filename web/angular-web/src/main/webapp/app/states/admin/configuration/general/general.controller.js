import EVENTS from '../../../../constants/artifacts_events.constants';
import API from '../../../../constants/api.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminConfigurationGeneralController {

    constructor($scope, $q, $timeout, $stateParams, FileUploader, ArtifactoryNotifications, GeneralConfigDao, FooterDao, ArtifactoryEventBus, ArtifactoryModelSaver, ArtifactActionsDao, ArtifactoryModal, ArtifactoryFeatures) {
        this.$scope = $scope;
        this.$q = $q;
        this.$timeout = $timeout;
        this.$stateParams = $stateParams;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactActionsDao = ArtifactActionsDao;
        this.generalConfigDao = GeneralConfigDao;
        this.footerDao = FooterDao;
        this.FileUploader = FileUploader;
        this.modal = ArtifactoryModal;
        this.logoType = 'File';
        this.defaultLogoUrl = 'images/artifactory_logo.svg';
        this.logoEndPoint = `${API.API_URL}/generalConfig/logo`;
        this.TOOLTIP = TOOLTIP.admin.configuration.general;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['generalConfigData']);
        this.features = ArtifactoryFeatures;

        this.deleteUserLogo = false;

        this._initUploader();

        this._getGeneralConfigData();

    }
    customMessageToggle() {
        this.toggleColorPicker();
    }
    toggleColorPicker() {
        $('.color-picker-input').prop('disabled', !this.generalConfigData.systemMessageEnabled);
    }
    _getGeneralConfigData() {
        this.generalConfigDao.get().$promise.then((data) => {
            this.generalConfigData = data;
            this.blockReplications = data.blockPushReplications || data.blockPullReplications;
            this.generalConfigData.systemMessageTitleColor = this.generalConfigData.systemMessageTitleColor.toUpperCase();
            this.artifactoryModelSaver.save();
            this._getCurrentImage();
            if (this.generalConfigData.systemMessageEnabled == false) {
                this.toggleColorPicker();
            }
        });
    }

    _getCurrentImage() {
        this._userLogoExists().then(()=>{
            $(".artifactory-logo img")[0].src = this.logoEndPoint;

        })
            .catch(() => {
                if (this.generalConfigData.logoUrl) {
                    this.logoUrlInput = this.generalConfigData.logoUrl;
                    $(".artifactory-logo img")[0].src = this.generalConfigData.logoUrl;
                }
                else {
                    $(".artifactory-logo img")[0].src = this.defaultLogoUrl;
                }
            });
    }

    _updateGeneralConfigData() {
        this.generalConfigDao.update(this.generalConfigData).$promise.then((data) => {
            this.artifactoryModelSaver.save();
            this.artifactoryEventBus.dispatch(EVENTS.FOOTER_DATA_UPDATED);
        });
    }

    _deleteUploadedPicture() {
        this.generalConfigDao.deleteLogo().$promise.then((data) => {
//            console.log(data);
        });
    }

    _initUploader() {
        this.uploader = new this.FileUploader();

        this.uploader.url = this.logoEndPoint;
        this.uploader.onSuccessItem = this.onUploadSuccess.bind(this);
    }

    isSelectedLogoType(type) {
        return this.logoType === type;
    }

    onUploadSuccess() {
//        console.log('onUploadSuccess');
//        this.generalConfigData.logoUrl = this.logoEndPoint;
        this.logoUrlInput = undefined;
        this._updateGeneralConfigData();
    }

    onAfterAddingFile(fileItem) {
        this.assertImage(fileItem._file).then(()=> {
            this.logoFile = fileItem.file.name;
            this.showPreview(fileItem._file);
        }).catch((err)=> {
            this.artifactoryNotifications.create({error: err});
            this.uploader.clearQueue();
        });
    }

    assertImage(file) {

        let deferred = this.$q.defer();

        let reader = new FileReader();
        reader.onload = function (e) {
            let buffer = reader.result;
            let uInt8View = new Uint8Array(buffer);
            let int32Sample = uInt8View[3] + uInt8View[2] * 256 + uInt8View[1] * (256 * 256) + uInt8View[0] * (256 * 256 * 256);

            switch (int32Sample) {
                case 2303741511: //png
                case 1195984440: //gif
                case 1112360694: //bmp
                case 4292411360: case 4292411361: //jpg
                //case 1010792557: case 1014199911: //svg
                    deferred.resolve();
                    break;
                default:
                    deferred.reject('Not an image file!');
                    break;
            }
        };
        reader.readAsArrayBuffer(file);

        return deferred.promise;

    }

    showPreview(file) {
        if (typeof FileReader !== "undefined" && (/image/i).test(file.type)) {
            let img = $(".artifactory-logo img")[0];
            let reader = new FileReader();
            reader.onload = (((theImg) => {
                return (evt) => {
                    theImg.src = evt.target.result;
                }
            })(img));
            reader.readAsDataURL(file);
        }
    }


    clear() {
        this.generalConfigData.customUrlBase = '';

        this.generalConfigData.logoUrl = '';//this.defaultLogoUrl;
        this.logoUrlInput = undefined;
        this.uploader.clearQueue();
        this.logoFile = undefined;

        this.deleteUserLogo = true;

        this.$timeout(()=>{
            $(".artifactory-logo img")[0].src = this.defaultLogoUrl;
        });

    }
    save() {
        if (this.isSelectedLogoType('File') && this.uploader.queue.length) {
            this.uploader.uploadAll();
        }
        else {
            if (this.deleteUserLogo) this._deleteUploadedPicture();
            this._updateGeneralConfigData();
        }
    }

    cancel() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.clear();
            this._getGeneralConfigData();
        });
    }

    onChangeLogoUrl() {
        let form = this.$scope.formGeneral;
        if (this.logoUrlInput && !form.logoUrlInput.$invalid) {
            this._imageExists(this.logoUrlInput)
            .then(()=>{
                    this.generalConfigData.logoUrl = this.logoUrlInput;
                    this.deleteUserLogo = true;
                })
            .catch((err)=>console.log(err));

        }
    }

    _userLogoExists() {
        let deferred = this.$q.defer();
        this.footerDao.get(true).then(footerData => {
            if (footerData.userLogo) {
                deferred.resolve();
            }
            else {
                deferred.reject();
            }
        });
        return deferred.promise;
    }

    _imageExists(url) {
        let deferred = this.$q.defer();
        let img = new Image();
        img.onload = () => {
            deferred.resolve();
        };
        img.onerror = () => {
            deferred.reject('no image found');
        };
        img.src = url;
        return deferred.promise;
    }

    emptyTrashcan() {
        this.modal.confirm('Are you sure you want to empty the trash can?', 'Empty Trash',
            {confirm: 'Empty Trash'})
            .then(() => this.artifactActionsDao.perform({action: 'emptytrash'}, {}));
    }

    onChangeBlockReplications() {
        this.generalConfigData.blockPushReplications = this.blockReplications;
        this.generalConfigData.blockPullReplications = this.blockReplications;
    }
    onChangePushPullReplications() {
        this.blockReplications = this.generalConfigData.blockPushReplications || this.generalConfigData.blockPullReplications
    }
}
