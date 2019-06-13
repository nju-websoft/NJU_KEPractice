var propertyLabel = function (property) {
    property = property.replace('http://', '');
    if (property.lastIndexOf('#') >= 0) {
        return property.substring(property.lastIndexOf('#') + 1);
    } else if (property.lastIndexOf(':') >= 0) {
        return property.substring(property.lastIndexOf(':') + 1);
    } else {
        return property.substring(property.lastIndexOf('/') + 1);
    }
};

var queryView = {
    model: null,
    limit: 10,
    init: function () {
    	var parts = location.href.split('#'), path, params;
    	path = parts[0];
    	params = {'query': '', 'page': 0};
    	if (parts.length > 1) {
        	parts[1].split('&').forEach(function (part) {
        		if (part.indexOf('=') >= 0) {
            		params[part.split('=')[0]] = decodeURIComponent(part.split('=')[1])
        		}
        	});
        	console.log(path, params);
        	$.get(path + '/../query?query=' + encodeURIComponent(params['query'])).then(
        		function (model) {
        			queryView.model = model
        			if (!('keywords' in model)) {
        				queryView.model.keywords = [params['query']];
        			}
        			queryView.render();
        		})
    	}
    },
    render: function (page) {
        page = page | 0;
        this.renderPageList(page, Math.ceil(this.model.hittingInstances.length / this.limit));
        this.renderResults(this.model.hittingInstances.slice(page * this.limit, (page + 1) * this.limit), this.model.keywords);
        window.scrollTo(window.scrollX, 0);
    },
    renderPageList: function (page, pageCount) {
        that = this;
        $pagination = $('#pagination');
        $pagination.html('');
        for (i = 0; i < pageCount; i++) {
            if (i == page) {
                $pagination.append('<div>' + (i + 1) + '</div>')
            } else {
                $pagination.append('<div><a href="javascript: void(0);">' + (i + 1) + '</a></div>')
            }
        }

        $('a', $pagination).click(function (e) {
            that.render(e.target.innerHTML - 1)
        });
    },
    renderResults: function (hittingInstances, keywords) {
        var hittingRegExp = new RegExp(keywords.join('|'), 'ig'), $resultList = $('#result-list');

        $resultList.html('');
        hittingInstances.forEach(function (instance) {
            content = '';
            content += '<h2 class="instance"><a href="' + instance.iri + '">' + instance.label + '</a></h2>';
            types = '';
            content += '<h3><span class="field">type</span>' +
                instance.types.map(function (type) {
                    return '<span>' + type.label + '</span>';
                }).join(', ') + '</h3>';


            for (key in instance.attributes) {
                instance.attributes[key].forEach(function (value) {
                    content += '<p><span class="field">' + propertyLabel(key) + '</span>' +
                        value.replace(hittingRegExp, function (e) {
                            return '<span class="hitting">' + e + '</span>'
                        }) + '</p>';
                });
            }

            $resultList.append('<li>' + content + '</li>');
        });
    }
}

var instanceView = {
    model: null,
    render: function () {
        var instance = this.model, content, key;
        $('h2.instance>a').attr('href', instance.iri).html(instance.iri);

        content = '';
        for (key in instance.attributes) {
            instance.attributes[key].forEach(function (value) {
                content += '<p><span class="field">' + propertyLabel(key) + '</span>' +
                    value + '</p>';
            });
        }

        $('#attributes').html(content);

        content = '';
        for (key in instance.relations) {
            instance.relations[key].forEach(function (value) {
                content += '<p><span class="field">' + propertyLabel(key) + '</span>' +
                    '<a href="' + value.iri + '">' + value.label + '</a>' + '</p>';
            });
        }

        $('#relations').html(content);
    }
}