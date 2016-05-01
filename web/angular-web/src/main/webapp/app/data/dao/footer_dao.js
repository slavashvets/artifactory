import EVENTS from '../../constants/artifacts_events.constants';
const VERSION_INFO_KEY = 'VERSION_INFO';
export class FooterDao {
	constructor(RESOURCE, ArtifactoryDaoFactory, ArtifactoryStorage, $timeout, ArtifactoryEventBus) {
		this.storage = ArtifactoryStorage;
        this.$timeout = $timeout;
        this.ArtifactoryEventBus = ArtifactoryEventBus;
    	this._resource = ArtifactoryDaoFactory()
            .setPath(RESOURCE.FOOTER)
            .getInstance();
    }

    get(force = false) {

/*
        let now = (new Date()).getTime();
        if (this.lastGet && now - this.lastGet > 1000 && !this._info) force = true;
        this.lastGet = now;
*/

        if (!this.cached || force) {
            this.cached = this._resource.get().$promise
                    .then(info => this._info = info);
        }

        //Fix for RTFACT-9873
        if (!this._info) {
            this.$timeout(()=> {
                if (!this._info) {
                    this.get(true).then(()=> {
                        this.ArtifactoryEventBus.dispatch(EVENTS.FOOTER_DATA_UPDATED);
                    });
                }
            }, 400);
        }

        return this.cached;
    }

    getInfo() {
        return this._info;
    }
}
